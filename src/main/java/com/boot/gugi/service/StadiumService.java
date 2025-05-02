package com.boot.gugi.service;

import com.boot.gugi.base.dto.StadiumDTO;
import com.boot.gugi.model.Food;
import com.boot.gugi.model.Stadium;
import com.boot.gugi.model.StadiumFood;
import com.boot.gugi.repository.FoodRepository;
import com.boot.gugi.repository.RedisRepository;
import com.boot.gugi.repository.StadiumFoodRepository;
import com.boot.gugi.repository.StadiumRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StadiumService {

    private final RedisRepository redisRepository;
    private final StadiumRepository stadiumRepository;
    private final FoodRepository foodRepository;
    private final StadiumFoodRepository stadiumFoodRepository;
    private final S3Service s3Service;
    private final RedisTemplate<String, String> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(StadiumService.class);

    public StadiumDTO.StadiumResponse getStadiumInfo(Integer stadiumCode) {
        StadiumDTO.StadiumResponse stadiumResponse = new StadiumDTO.StadiumResponse();

        StadiumDTO.StadiumInfo stadiumInfo = redisRepository.findStadium(stadiumCode);
        Stadium stadium = stadiumRepository.findByStadiumCode(stadiumCode);

        if (stadiumInfo != null) {
            stadiumResponse.setStadiumInfo(stadiumInfo);
        } else {
            if (stadium == null) {
                throw new EntityNotFoundException("Stadium not found for code in DB: " + stadiumCode);
            }

            StadiumDTO.StadiumInfo stadiumDTO = new StadiumDTO.StadiumInfo();
            stadiumDTO.setStadiumName(stadium.getStadiumName());
            stadiumDTO.setStadiumLocation(stadium.getStadiumLocation());
            stadiumDTO.setTeamName(stadium.getTeamName());

            redisRepository.syncStadiumToRedis(stadium);
            stadiumResponse.setStadiumInfo(stadiumDTO);
        }

        String foodRedisKey = "food-code:" + stadiumCode;
        long mongoFoodCount = stadiumFoodRepository.countByStadiumCode(stadiumCode);
        long redisFoodCount = redisTemplate.opsForHash().size(foodRedisKey);

        if (redisFoodCount == 0 || isSyncRequired(mongoFoodCount, redisFoodCount)) {
            logger.info("Food Redis 동기화 필요. {}. MongoDB: {}개, Redis: {}개", stadiumCode, mongoFoodCount, redisFoodCount);

            List<Food> dbFoods = stadium.getFoods().stream().toList();
            Set<StadiumDTO.FoodResponse> foodDTOs = dbFoods.stream()
                    .map(food -> new StadiumDTO.FoodResponse(food.getFoodName(), food.getFoodLocation(), food.getFoodImg()))
                    .collect(Collectors.toSet());

            redisRepository.syncFoodToRedis(dbFoods, foodRedisKey);
            stadiumResponse.setFoodList(foodDTOs);
        } else {
            Set<StadiumDTO.FoodResponse> foodDTOs = redisRepository.findFood(stadiumCode);
            stadiumResponse.setFoodList(foodDTOs);
        }

        return stadiumResponse;
    }

    private boolean isSyncRequired(long mongoCount, long redisCount) {
        return redisCount != mongoCount;
    }

    public void saveStadiumInfo(StadiumDTO.StadiumRequest stadiumRequest) {

        Stadium savedStadium = createStadiumDetails(stadiumRequest);
        redisRepository.saveStadium(savedStadium);
        stadiumRepository.save(savedStadium);
    }

    private Stadium createStadiumDetails(StadiumDTO.StadiumRequest stadiumRequest) {
        return Stadium.builder()
                .stadiumCode(stadiumRequest.getStadiumCode())
                .stadiumName(stadiumRequest.getStadiumName())
                .stadiumLocation(stadiumRequest.getStadiumLocation())
                .teamName(stadiumRequest.getTeamName())
                .build();
    }

    public void saveFoodInfo(StadiumDTO.FoodRequest foodDetails, MultipartFile foodImg) {

        String uploadedFoodUrl = s3Service.uploadImg(foodImg, null);
        Food savedFood = createFoodDetails(foodDetails, uploadedFoodUrl);
        redisRepository.saveFood(savedFood, foodDetails.getStadiumCode());
        foodRepository.save(savedFood);
    }

    private Food createFoodDetails(StadiumDTO.FoodRequest foodDetails, String foodImgURL) {
        Food food = Food.builder()
                .foodName(foodDetails.getFoodName())
                .foodImg(foodImgURL)
                .foodLocation(foodDetails.getFoodLocation())
                .build();

        Stadium stadium = stadiumRepository.findByStadiumCode(foodDetails.getStadiumCode());
        StadiumFood stadiumFood = StadiumFood.builder()
                .food(food)
                .stadium(stadium)
                .build();

        stadium.getStadiumFoods().add(stadiumFood);
        return food;
    }
}