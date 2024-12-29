package com.boot.gugi.repository;

import com.boot.gugi.model.MatePost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatePostRepository extends JpaRepository<MatePost, UUID> {

}
