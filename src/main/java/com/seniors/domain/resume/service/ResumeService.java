package com.seniors.domain.resume.service;

import com.seniors.common.dto.CustomSlice;
import com.seniors.common.exception.type.*;
import com.seniors.config.S3Uploader;
import com.seniors.domain.notification.service.NotificationService;
import com.seniors.domain.resume.dto.*;
import com.seniors.domain.resume.dto.ResumeDto.SaveResumeReq;
import com.seniors.domain.resume.entity.*;
import com.seniors.domain.resume.repository.ResumeRepository;
import com.seniors.domain.resume.repository.ResumeViewRepository;
import com.seniors.domain.users.entity.Users;
import com.seniors.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UsersRepository usersRepository;
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final ResumeViewRepository resumeViewRepository;

    private final S3Uploader s3Uploader;

    @Transactional
    public Long addResume(SaveResumeReq resumeReq, MultipartFile image, Long userId) throws IOException {
        if (resumeRepository.findByUsersId(userId).isPresent()) {
            throw new ConflictException("이미 해당 유저의 이력서가 존재합니다.");
        }

        resumeReq.getCareers().stream()

                .filter(saveCareerReq -> saveCareerReq.getEndedAt() != null && saveCareerReq.getIsAttendanced() == true)
                .findAny()
                .ifPresent(saveCareerReq -> {
                    throw new BadRequestException("퇴사연도를 입력하심면 재직중 여부를 체크하실 수 없습니다.");
                });

        resumeReq.getEducations().stream()
                .filter(saveEducationReq -> saveEducationReq.getEndedAt() != null && saveEducationReq.getIsProcessed() == true)
                .findAny()
                .ifPresent(saveEducationReq1 -> {
                    throw new BadRequestException("종료연도를 입력하시면 진행중 여부를 체크하실 수 없습니다.");
                });

        Users user = usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("회원이 존재하지 않습니다.")
        );

        Resume resume = Resume.of(resumeReq, user);
        if (image != null) {
            String photoUrl = s3Uploader.upload(image, "resumes");
            resume.uploadPhotoUrl(photoUrl);
        } else {
            resume.uploadPhotoUrl(null);
        }

        resumeReq.getCareers().stream()
                .map(Career::from)
                .forEach(resume::addCareer);

        resumeReq.getCertificates().stream()
                .map(Certificate::from)
                .forEach(resume::addCertificate);

        resumeReq.getEducations().stream()
                .map(Education::from)
                .forEach(resume::addEducation);

        Resume savedResume = resumeRepository.save(resume);
        return savedResume.getId();
    }

    @Transactional(readOnly = true)
    public ResumeDto.GetResumeRes findResume(Long resumeId, Long userId) {
        Users user = usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("회원이 존재하지 않습니다.")
        );
        Resume resume = resumeRepository.findById(resumeId).orElseThrow(
                () -> new NotFoundException("이력서가 존재하지 않습니다.")
        );
        Optional<ResumeView> findResumeView = resumeViewRepository.findByUsersAndResume(user, resume);
        if (!findResumeView.isPresent()) {
            saveToRedis(resumeId, userId);
        }
        return ResumeDto.GetResumeRes.from(resume);
    }

    @Transactional(readOnly = true)
    public ResumeDto.GetResumeRes findMyResume(Long userId) {
        Users user = usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("회원이 존재하지 않습니다.")
        );

        Optional<Resume> resume = resumeRepository.findByUsersId(user.getId());
        if (resume.isEmpty()) {
            return null;
        }
        return ResumeDto.GetResumeRes.from(resume.get());
    }

    @Transactional(readOnly = true)
    public CustomSlice<ResumeDto.GetResumeByQueryDslRes> findResumeList(Pageable pageable, Long lastId, Long userId) {
        Users user = usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("회원이 존재하지 않습니다.")
        );
        Slice<ResumeDto.GetResumeByQueryDslRes> result = resumeRepository.findResumeList(pageable, lastId, user.getId());
        return CustomSlice.from(result);
    }

    @Transactional
    public void modifyResume(Long resumeId, ResumeDto.ModifyResumeReq resumeReq, MultipartFile image, Long userId) throws IOException {
        Resume resume = resumeRepository.findById(resumeId).orElseThrow(
                () -> new NotFoundException("이력서가 존재하지 않습니다.")
        );

        resumeReq.getCareers().stream()
                .filter(modifyCareerReq -> modifyCareerReq.getEndedAt() != null && modifyCareerReq.getIsAttendanced() == true)
                .findAny()
                .ifPresent(modifyCareerReq -> {
                    throw new BadRequestException("퇴사연도를 입력하심면 재직중 여부를 체크하실 수 없습니다.");
                });

        resumeReq.getEducations().stream()
                .filter(modifyEducationReq -> modifyEducationReq.getEndedAt() != null && modifyEducationReq.getIsProcessed() == true)
                .findAny()
                .ifPresent(modifyCareerReq -> {
                    throw new BadRequestException("종료연도를 입력하시면 진행중 여부를 체크하실 수 없습니다.");
                });

        Users user = usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("회원이 존재하지 않습니다.")
        );

        if (!resume.getUsers().getId().equals(user.getId())) {
            throw new ForbiddenException("수정 권한이 없습니다.");
        }

        if (image != null) {
            String photoUrl = s3Uploader.upload(image, "resumes");
            resume.update(resumeReq, photoUrl);
        } else {
            String photoUrl = null;
            resume.update(resumeReq, photoUrl);
        }

        resume.getCareers().clear();
        resume.getCertificates().clear();
        resume.getEducations().clear();

        resumeReq.getCareers().stream()
                .map(Career::from)
                .forEach(resume::addCareer);

        resumeReq.getCertificates().stream()
                .map(Certificate::from)
                .forEach(resume::addCertificate);

        resumeReq.getEducations().stream()
                .map(Education::from)
                .forEach(resume::addEducation);
    }

    @Transactional
    public void removeResume(Long resumeId, Long userId) {
        Resume resume = resumeRepository.findById(resumeId).orElseThrow(
                () -> new NotFoundException("이력서가 존재하지 않습니다.")
        );
        Users user = usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("회원이 존재하지 않습니다.")
        );
        if (!resume.getUsers().getId().equals(user.getId())) {
            throw new ForbiddenException("삭제 권한이 없습니다.");
        }
        resumeRepository.delete(resume);
    }


    @Transactional(readOnly = true)
    public List<ViewerInfoDto.GetViewerInfoRes> findResumeViewerList(Long userId) {

        Resume resume = resumeRepository.findByUsersId(userId).orElseThrow(
                () -> new NotFoundException("이력서가 존재하지 않습니다.")
        );

        Optional<List<ResumeView>> resumeViewList = resumeViewRepository.findByResumeId(resume.getId());
        if (resumeViewList.isEmpty()) {
            return null;
        }
        List<ViewerInfoDto.GetViewerInfoRes> viewerInfoList = new ArrayList<>();

        for (ResumeView resumeView : resumeViewList.get()) {
            viewerInfoList.add(ViewerInfoDto.GetViewerInfoRes.from(resumeView));
        }
        return viewerInfoList;
    }

    @Transactional
    public void saveToRedis(Long resumeId, Long userId) {
        String redisKeyForUsers = "resume:view:" + resumeId;
        String redisKeyForCnt = "resume:cnt" + resumeId;

        redisTemplate.opsForSet().add(redisKeyForUsers, userId);
        redisTemplate.opsForValue().increment(redisKeyForCnt);
    }

    @Scheduled(cron = "0 0/3 * * * ?")
    @SchedulerLock(
            name = "scheduler_lock",
            lockAtLeastFor = "PT10S",
            lockAtMostFor = "PT10S"
    )
    @Transactional
    public void updateResumeView() {
        // 1. Redis에서 resume:view:* 패턴의 키를 조회
        Set<String> resumeViewKeys = redisTemplate.keys("resume:view:*");
        if (resumeViewKeys != null) {
            for (String key : resumeViewKeys) {
                Long resumeId = Long.parseLong(key.split(":")[2]);
                Set<Object> userIds = redisTemplate.opsForSet().members(key);

                // ResumeView 객체를 생성하여 저장
                for (Object userIdObj : userIds) {
                    Long userId = Long.parseLong(userIdObj.toString());
                    Resume resume = resumeRepository.findById(resumeId).orElseThrow();
                    Users user = usersRepository.findById(userId).orElseThrow();
                    ResumeView resumeView = ResumeView.of(resume, user);
                    resumeViewRepository.save(resumeView);
                }
            }
            redisTemplate.delete(resumeViewKeys);
        }

        // 2. Redis에서 resume:cnt 패턴의 키를 조회
        Set<String> cntKeys = redisTemplate.keys("resume:cnt*");
        if (cntKeys != null) {
            for (String key : cntKeys) {
                Long resumeId = Long.parseLong(key.split("cnt")[1]);
                String cntValue = (String) redisTemplate.opsForValue().get(key);
                if (cntValue != null) {
                    int viewCount = Integer.parseInt(cntValue);
                    // Resume 객체를 조회하고 조회 수를 업데이트
                    Resume resume = resumeRepository.findById(resumeId).orElseThrow();
                    resume.updateViewCnt(viewCount);
                }
            }
            redisTemplate.delete(cntKeys);
        }
    }
}