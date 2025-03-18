package com.boot.gugi.service;

import com.boot.gugi.base.dto.StadiumDTO;
import com.boot.gugi.model.Food;
import com.boot.gugi.model.Stadium;
import com.boot.gugi.model.StadiumFood;
import com.boot.gugi.repository.FoodRepository;
import com.boot.gugi.repository.RedisRepository;
import com.boot.gugi.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StadiumService {

    private final RedisRepository redisRepository;
    private final StadiumRepository stadiumRepository;
    private final FoodRepository foodRepository;
    private final S3Service s3Service;

    public StadiumDTO.StadiumResponse getStadiumInfo(Integer stadiumCode) {

        StadiumDTO.StadiumResponse stadiumInfo = redisRepository.findStadium(stadiumCode);
        if (stadiumInfo == null) {
            Stadium stadium = stadiumRepository.findByStadiumCode(stadiumCode);

            StadiumDTO.StadiumResponse stadiumDTO = new StadiumDTO.StadiumResponse();
            stadiumDTO.setStadiumName(stadium.getStadiumName());
            stadiumDTO.setStadiumLocation(stadium.getStadiumLocation());
            stadiumDTO.setTeamName(stadium.getTeamName());

            Set<StadiumDTO.FoodResponse> foodDTOs = stadium.getFoods().stream()
                    .map(food -> new StadiumDTO.FoodResponse(food.getFoodName(),food.getFoodLocation(), food.getFoodImg()))
                    .collect(Collectors.toSet());
            stadiumDTO.setFoodList(foodDTOs);

            return stadiumDTO;
        }
        return stadiumInfo;
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