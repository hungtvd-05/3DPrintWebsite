package com.web.repository;

import com.web.model.WebInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebInfoRepository extends JpaRepository<WebInfo, Integer> {

    WebInfo findFirstByOrderByIdAsc();

}
