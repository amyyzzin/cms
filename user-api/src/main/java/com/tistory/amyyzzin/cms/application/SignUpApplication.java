package com.tistory.amyyzzin.cms.application;

import com.tistory.amyyzzin.cms.client.MailgunClient;
import com.tistory.amyyzzin.cms.client.mailgun.SendMailForm;
import com.tistory.amyyzzin.cms.domain.SignUpForm;
import com.tistory.amyyzzin.cms.domain.model.Customer;
import com.tistory.amyyzzin.cms.domain.model.Seller;
import com.tistory.amyyzzin.cms.exception.CustomException;
import com.tistory.amyyzzin.cms.exception.ErrorCode;
import com.tistory.amyyzzin.cms.service.customer.SignUpCustomerService;
import com.tistory.amyyzzin.cms.service.seller.SellerService;
import java.time.LocalDateTime;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignUpApplication {

    private final MailgunClient mailgunClient;
    private final SignUpCustomerService signUpCustomerService;
    private final SellerService sellerService;

    Properties properties = new Properties();


    public void customerVerify(String email, String code) {
        signUpCustomerService.verifyEmail(email, code);
    }

    public String customerSignUp(SignUpForm form) {

        if (signUpCustomerService.isEmailExist(form.getEmail())) {
            throw new CustomException(ErrorCode.ALREADY_REGISTER_USER);
        } else if (!signUpCustomerService.isMailPattern(form.getEmail())) {
            throw new CustomException(ErrorCode.NOT_MATCH_EMAIL_PATTERN);
        } else if (!signUpCustomerService.isPhonePattern(form.getPhone())) {
            throw new CustomException(ErrorCode.NOT_MATCH_PHONE_PATTERN);
        } else if (!signUpCustomerService.isPasswordPattern(form.getPassword())) {
            throw new CustomException(ErrorCode.NOT_MATCH_PASSWORD_PATTERN);
        } else {
            Customer c = signUpCustomerService.signUp(form);

            String code = getRandomCode();

            SendMailForm sendMailForm = SendMailForm.builder()
                .from((properties.get("senderMail")).toString())
                .to(form.getEmail())
                .subject("Verification Email")
                .text(getVerificationEmailBody(c.getEmail(), c.getName(), "customer", code))
                .build();

            mailgunClient.sendEmail(sendMailForm);
            signUpCustomerService.changeCustomerValidateEmail(c.getId(), code);
            return "회원가입에 성공하였습니다.";
        }
    }

    public void sellerVerify(String email, String code) {
        sellerService.verifyEmail(email, code);
    }

    public String sellerSignUp(SignUpForm form) {
        if (sellerService.isEmailExist(form.getEmail())) {
            throw new CustomException(ErrorCode.ALREADY_REGISTER_USER);
        } else {
            Seller s = sellerService.signUp(form);
            LocalDateTime now = LocalDateTime.now();

            String code = getRandomCode();
            SendMailForm sendMailForm = SendMailForm.builder()
                .from((properties.get("senderMail")).toString())
                .to(form.getEmail())
                .subject("Verification Email")
                .text(getVerificationEmailBody(s.getEmail(), s.getName(), "seller", code))
                .build();
            mailgunClient.sendEmail(sendMailForm);

            sellerService.changeSellerValidateEmail(s.getId(), code);
            return "회원가입에 성공하였습니다.";
        }
    }

    private String getRandomCode() {
        return RandomStringUtils.random(10, true, true);
    }

    private String getVerificationEmailBody(String email, String name, String type, String code) {
        StringBuilder builder = new StringBuilder();
        return builder.append("Hello ")
            .append(name)
            .append("! Please Click Link for verification.\n\n ")
            .append(properties.get("signUpPath") + type + "/verify/?email=")
            .append(email)
            .append("&code=")
            .append(code).toString();
    }


}
