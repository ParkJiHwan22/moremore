package com.ssafy.clubservice.club.infrastructure.repository;

import com.ssafy.clubservice.club.infrastructure.repository.entity.ClubEntity;
import com.ssafy.clubservice.club.mapper.ClubObjectMapper;
import com.ssafy.clubservice.club.service.domain.Club;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClubRepositoryImpl implements ClubRepository {
    private final ClubMybatisMapper clubMybatisMapper;
    private final ClubObjectMapper clubObjectMapper;
    @Override
    public Club save(Club club) {
        ClubEntity entity = clubObjectMapper.toEntity(club);
        clubMybatisMapper.save(entity);
        return clubObjectMapper.fromEntity(entity);
    }

    @Override
    public Club findById(Long clubId) {
        ClubEntity entity = clubMybatisMapper.findById(clubId);
        return clubObjectMapper.fromEntity(entity);
    }

    @Override
    public Club update(Club club) {
        ClubEntity entity = clubObjectMapper.toEntity(club);
        clubMybatisMapper.update(entity);
        return clubObjectMapper.fromEntity(entity);
    }

    @Override
    public Club findByClubCode(String clubCode) {
        ClubEntity entity = clubMybatisMapper.findByClubCode(clubCode);
        return clubObjectMapper.fromEntity(entity);
    }

    @Override
    public Club findWithParticipantsByClubCode(String clubCode) {
        return clubMybatisMapper.findWithParticipantsByClubCode(clubCode);
    }
}
