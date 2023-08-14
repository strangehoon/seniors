package com.seniors.domain.resume.service;

import com.seniors.common.dto.CustomSlice;
import com.seniors.common.dto.DataResponseDto;
import com.seniors.common.exception.type.BadRequestException;
import com.seniors.common.exception.type.NotAuthorizedException;
import com.seniors.common.exception.type.NotFoundException;
import com.seniors.config.S3Uploader;
import com.seniors.domain.notification.service.NotificationService;
import com.seniors.domain.resume.dto.CareerDto;
import com.seniors.domain.resume.dto.CertificateDto;
import com.seniors.domain.resume.dto.EducationDto;
import com.seniors.domain.resume.dto.ResumeDto;
import com.seniors.domain.resume.dto.ResumeDto.SaveResumeReq;
import com.seniors.domain.resume.entity.*;
import com.seniors.domain.resume.repository.ResumeRepository;
import com.seniors.domain.resume.repository.ResumeViewRepository;
import com.seniors.domain.users.entity.Users;
import com.seniors.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UsersRepository usersRepository;
    private final NotificationService notificationService;

    private final ResumeViewRepository resumeViewRepository;

    @Value("${photo.url}")
    private String photoUrl;

    private final S3Uploader s3Uploader;
    @Transactional
    public Long addResume(SaveResumeReq resumeReq, Long userId) throws IOException {
        if (resumeRepository.findByUsersId(userId).isPresent()) {
            throw new BadRequestException("이미 해당 유저의 이력서가 존재합니다.");
        }

        for(CareerDto.saveCareerReq saveCareerReq : resumeReq.getCareerList()){
            if(saveCareerReq.getEndedAt()!= null && saveCareerReq.getIsAttendanced()==true){
                throw new BadRequestException("퇴사연도를 입력하심면 재직중 여부를 체크하실 수 없습니다.");
            }
        }

        for(EducationDto.saveEducationReq saveEducationReq : resumeReq.getEducationList()) {
            if(saveEducationReq.getEndedAt()!= null && saveEducationReq.getIsProcessed()==true){
                throw new BadRequestException("종료연도를 입력하시면 진행중 여부를 체크하실 수 없습니다.");
            }
        }

        Users user =  usersRepository.findById(userId).orElseThrow(
                () -> new NotAuthorizedException("유효하지 않은 회원입니다.")
        );

        if(!resumeReq.getImage().isEmpty()) {
            photoUrl = s3Uploader.upload(resumeReq.getImage(), "resumes");
        }

        Resume resume = Resume.of(resumeReq, user);
        resume.uploadPhotoUrl(photoUrl);

        for(CareerDto.saveCareerReq saveCareerReq  : resumeReq.getCareerList()){
            Career career = Career.from(saveCareerReq);
            resume.addCareer(career);
        }
        for(CertificateDto.saveCertificateReq saveCertificateReq : resumeReq.getCertificateList()){
            Certificate certificate = Certificate.from(saveCertificateReq);
            resume.addCertificate(certificate);
        }
        for(EducationDto.saveEducationReq saveEducationReq : resumeReq.getEducationList()){
            Education education = Education.from(saveEducationReq);
            resume.addEducation(education);
        }
        Resume savedResume = resumeRepository.save(resume);
        return savedResume.getId();
    }

    @Transactional
    public ResumeDto.GetResumeRes findResume(Long resumeId, Long userId) {
        Users user =  usersRepository.findById(userId).orElseThrow(
                () -> new NotAuthorizedException("유효하지 않은 회원입니다.")
        );
        Resume resume =  resumeRepository.findById(resumeId).orElseThrow(
                () -> new NotFoundException("이력서가 존재하지 않습니다.")
        );

        Optional<ResumeView> findResumeView = resumeViewRepository.findByUsersAndResume(user, resume);
        if (!findResumeView.isPresent()) {
            ResumeView resumeView = ResumeView.of(resume,user);
            resumeViewRepository.save(resumeView);
            resume.increaseViewCount();
        }

        if (!resume.getUsers().getId().equals(user.getId())) {
            notificationService.send(resume.getUsers(), resume, "누군가가 내 이력서를 조회했습니다!");
        }
        return ResumeDto.GetResumeRes.from(resume);
    }

    @Transactional
    public DataResponseDto<CustomSlice<ResumeDto.GetResumeByQueryDslRes>> findResumeList(Pageable pageable, Long lastId, Long userId){
        Users user =  usersRepository.findById(userId).orElseThrow(
                () -> new NotAuthorizedException("유효하지 않은 회원입니다.")
        );
        Slice<ResumeDto.GetResumeByQueryDslRes> result = resumeRepository.findResumeList(pageable, lastId, user.getId());


        return DataResponseDto.of(CustomSlice.from(result));
    }

    @Transactional
    public void modifyResume(Long resumeId, ResumeDto.ModifyResumeReq resumeReq, Long userId) throws IOException {
        Resume resume = resumeRepository.findById(resumeId).orElseThrow(
                () ->new NotFoundException("이력서가 존재하지 않습니다.")
        );

        for(CareerDto.modifyCareerReq modifyCareerReq : resumeReq.getCareerList()){
            if(modifyCareerReq.getEndedAt()!= null && modifyCareerReq.getIsAttendanced()==true){
                throw new BadRequestException("퇴사연도를 입력하심면 재직중 여부를 체크하실 수 없습니다.");
            }
        }

        for(EducationDto.modifyEducationReq modifyEducationReq : resumeReq.getEducationList()) {
            if(modifyEducationReq.getEndedAt()!= null && modifyEducationReq.getIsProcessed()==true){
                throw new BadRequestException("종료연도를 입력하시면 진행중 여부를 체크하실 수 없습니다.");
            }
        }

        Users user =  usersRepository.findById(userId).orElseThrow(
                () -> new NotAuthorizedException("유효하지 않은 회원입니다.")
        );

        if(resume.getUsers().getId()!=user.getId()){
            throw new NotAuthorizedException("수정 권한이 없습니다.");
        }

        if(!resumeReq.getImage().isEmpty()) {
            photoUrl = s3Uploader.upload(resumeReq.getImage(), "resumes");
        }

        resume.update(resumeReq, photoUrl);

        resume.getCareers().clear();
        resume.getCertificates().clear();
        resume.getEducations().clear();

        for(CareerDto.modifyCareerReq modifyCareerReq : resumeReq.getCareerList()){
            Career career = Career.from(modifyCareerReq);
            resume.addCareer(career);
        }

        for(CertificateDto.modifyCertificateReq modifyCertificateReq : resumeReq.getCertificateList()){
            Certificate certificate = Certificate.from(modifyCertificateReq);
            resume.addCertificate(certificate);
        }

        for(EducationDto.modifyEducationReq modifyEducationReq : resumeReq.getEducationList()) {
            Education education = Education.from(modifyEducationReq);
            resume.addEducation(education);
        }
    }

    @Transactional
    public void removeResume(Long resumeId, Long userId){
        Resume resume = resumeRepository.findById(resumeId).orElseThrow(
                () -> new NotFoundException("이력서가 존재하지 않습니다.")
        );
        Users user =  usersRepository.findById(userId).orElseThrow(
                () -> new NotAuthorizedException("유효하지 않은 회원입니다.")
        );
        if(resume.getUsers().getId()!=user.getId()){
            throw  new NotAuthorizedException("삭제 권한이 없습니다.");
        }
        resumeRepository.delete(resume);
    }
}
