package com.example.secdsp.modules.email.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ErrorCode;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.config.MailProperties;
import com.example.secdsp.modules.email.entity.EmailOtp;
import com.example.secdsp.modules.email.repository.EmailOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final EmailOtpRepository emailOtpRepository;
    private final MailProperties mailProperties;

    @Override
    public String generateOtp(String email) {

        String otp = generateRandomOtp();

        EmailOtp emailOtp = new EmailOtp();

        emailOtp.setEmail(email);
        emailOtp.setOtp(otp);
        emailOtp.setExpiryTime(
            OffsetDateTime.now()
                .plusMinutes(mailProperties.getOtpExpirationMinutes())
        );
        emailOtp.setUsed(false);
        emailOtp.setVerified(false);
        emailOtp.setCreatedAt(OffsetDateTime.now());
        emailOtp.setResendCount(0);

        emailOtpRepository.save(emailOtp);

        return otp;
    }

    @Override
    public String resendOtp(String email) {

        EmailOtp latestOtp = emailOtpRepository
            .findTopByEmailOrderByIdDesc(email)
            .orElseThrow(() ->
                             new ResourceNotFoundException("OTP", email)
            );

        OffsetDateTime now = OffsetDateTime.now();

        if (latestOtp.getCreatedAt()
            .plusSeconds(mailProperties.getResendCooldownSeconds())
            .isAfter(now)) {

            throw new BusinessException(
                ErrorCode.TOO_MANY_REQUESTS,
                "Please wait before requesting another OTP."
            );
        }

        if (latestOtp.getResendCount()
            >= mailProperties.getMaxResendAttempts()) {

            throw new BusinessException(
                ErrorCode.TOO_MANY_REQUESTS,
                "Maximum OTP resend attempts exceeded."
            );
        }

        String newOtp = generateRandomOtp();

        latestOtp.setOtp(newOtp);
        latestOtp.setExpiryTime(
            now.plusMinutes(
                mailProperties.getOtpExpirationMinutes()
            )
        );
        latestOtp.setCreatedAt(now);
        latestOtp.setResendCount(
            latestOtp.getResendCount() + 1
        );
        latestOtp.setUsed(false);
        latestOtp.setVerified(false);

        emailOtpRepository.save(latestOtp);

        return newOtp;
    }

    @Override
    public void validateOtp(String email, String otp) {

        EmailOtp emailOtp = emailOtpRepository
            .findTopByEmailOrderByIdDesc(email)
            .orElseThrow(() ->
                             new ResourceNotFoundException("OTP", email)
            );

        OffsetDateTime now = OffsetDateTime.now();

        if (emailOtp.isUsed()) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "OTP has already been used."
            );
        }

        if (emailOtp.getExpiryTime().isBefore(now)) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "OTP has expired."
            );
        }

        if (!emailOtp.getOtp().equals(otp)) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Invalid OTP."
            );
        }

        emailOtp.setUsed(true);
        emailOtp.setVerified(true);

        emailOtpRepository.save(emailOtp);
    }

    private String generateRandomOtp() {

        return String.valueOf(
            ThreadLocalRandom.current()
                .nextInt(100000, 1000000)
        );
    }
}