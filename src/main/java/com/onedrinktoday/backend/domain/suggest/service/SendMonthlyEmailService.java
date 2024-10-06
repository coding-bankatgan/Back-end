package com.onedrinktoday.backend.domain.suggest.service;

import com.onedrinktoday.backend.domain.drink.entity.Drink;
import com.onedrinktoday.backend.domain.member.entity.Member;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SendMonthlyEmailService {
  private final JavaMailSender emailSender;

  public SendMonthlyEmailService(JavaMailSender emailSender) {
    this.emailSender = emailSender;
  }

  // 매월 1일 전송할 특산주 추천 이메일
  public void sendMonthlyDrinkEmail(Member member, List<Drink> drinks) {
    String subject = "매월 추천드리는 특산주 3종!";
    String body = createMonthlyEmailBody(member.getName(), drinks);

    sendEmail(member.getEmail(), subject, body);
  }

  private void sendEmail(String to, String subject, String body) {
    MimeMessage message = emailSender.createMimeMessage();

    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(body, true);

      emailSender.send(message);
    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }

  private String createMonthlyEmailBody(String name, List<Drink> drinks) {
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html>")
        .append("<html lang=\"ko\">")
        .append("<head>")
        .append("<meta charset=\"UTF-8\">")
        .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        .append("<title>이 달의 특산주 추천 서비스 메일 발송</title>")
        .append("</head>")
        .append("<body style=\"width: 800px; margin: 50px auto; color: #24201A; font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', 'Apple Gothic', sans-serif; border-right: 1px solid #F3F4F8; border-bottom: 1px solid #F3F4F8; border-left: 1px solid #F3F4F8; letter-spacing: -0.6px;\">")
        .append("<header style=\"display: flex; justify-content: space-between; align-items: center; padding: 20px; border-top: 7px solid transparent; border-image: linear-gradient(to right, #FAE831, #FFB644) 1;\">")
        .append("<img src=\"https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdn%2Fd07jr5%2FbtsJV1t2kMh%2F9GY5hastcYhQk71sbKHY60%2Fimg.png\" alt=\"오늘한잔\" loading=\"lazy\" style=\"width: 140px;\">")
        .append("<img src=\"https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdn%2FbzHgBN%2FbtsJWQ6dSpN%2Fba1xDbUJgjFFd9JEoJsW11%2Fimg.png\" alt=\"오늘한잔\" loading=\"lazy\" style=\"width: 70px;\">")
        .append("</header>")
        .append("<h2 style=\"display: inline-block; margin: 0 0 20px 20px; box-shadow: inset 0 -10px #FAE831; font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', 'Apple Gothic', sans-serif;\">")
        .append(name).append("님을 위한 이 달의 특산주 3종 추천!</h2>")
        .append("<p style=\"padding: 0 20px; font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', 'Apple Gothic', sans-serif; font-size: 16px;\">")
        .append("이번 달 추천드리는 특산주 3종입니다:</p>");

    for (Drink drink : drinks) {
      sb.append("<table style=\"margin: 30px 20px; padding: 20px; background-color: #F3F4F8; border-radius: 15px;\">")
          .append("<thead>")
          .append("<tr style=\"height: 40px; font-size: 18px; text-align: left;\">")
          .append("<th>🍶 ").append(drink.getName()).append("</th>")
          .append("</tr>")
          .append("</thead>")
          .append("<tbody>")
          .append("<tr style=\"margin: 15px 0; font-size: 14px; display: flex; justify-content: flex-start; align-items: flex-start;\">")
          .append("<td class=\"desc-title\" style=\"font-weight: bold; display: inline-block; width: 60px;\"><span style=\"color: #FFB644;\">✔</span> 정보</td>")
          .append("<td class=\"desc\" style=\"width: calc(100% - 60px);\">").append(drink.getDescription()).append("</td>")
          .append("</tr>")
          .append("<tr style=\"margin: 15px 0; font-size: 14px; display: flex; justify-content: flex-start; align-items: flex-start;\">")
          .append("<td class=\"desc-title\" style=\"font-weight: bold; display: inline-block; width: 60px;\"><span style=\"color: #FFB644;\">✔</span> 도수</td>")
          .append("<td class=\"desc\" style=\"width: calc(100% - 60px);\">").append(drink.getDegree()).append("%</td>")
          .append("</tr>")
          .append("<tr style=\"margin: 15px 0; font-size: 14px; display: flex; justify-content: flex-start; align-items: flex-start;\">")
          .append("<td class=\"desc-title\" style=\"font-weight: bold; display: inline-block; width: 60px;\"><span style=\"color: #FFB644;\">✔</span> 당도</td>")
          .append("<td class=\"desc\" style=\"width: calc(100% - 60px);\">").append(drink.getSweetness()).append("</td>")
          .append("</tr>")
          .append("<tr style=\"margin: 15px 0; font-size: 14px; display: flex; justify-content: flex-start; align-items: flex-start;\">")
          .append("<td class=\"desc-title\" style=\"font-weight: bold; display: inline-block; width: 60px;\"><span style=\"color: #FFB644;\">✔</span> 가격</td>")
          .append("<td class=\"desc\" style=\"width: calc(100% - 60px);\">").append(drink.getCost()).append("원</td>")
          .append("</tr>")
          .append("</tbody>")
          .append("</table>");
    }

    sb.append("<p style=\"margin-bottom: 20px; padding: 0 20px; line-height: 1.8; font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', 'Apple Gothic', sans-serif; font-size: 16px;\">")
        .append("즐거운 한 달 되시길 바랍니다. 오늘 한 잔이 함께합니다! <br /> 감사합니다.")
        .append("</p>")
        .append("<p style=\"margin-bottom: 30px; padding: 0 20px; font-size: 14px; font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', 'Apple Gothic', sans-serif;\">")
        .append("이 메일은 <a href=\"https://coding-bankatgan.vercel.app\" style=\"color: #FFB644; text-decoration: none;\">오늘 한 잔 특산주 추천 서비스</a>에서 발송되었습니다.")
        .append("</p>")
        .append("<footer style=\"padding: 5px 0; font-size: 13px; text-align: center; background-color: #FFB644; color:#FFFFFF; font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', 'Apple Gothic', sans-serif;\">")
        .append("© 2024. One Drink Today Media All Rights Reserved.")
        .append("</footer>")
        .append("</body>")
        .append("</html>");

    return sb.toString();
  }
}