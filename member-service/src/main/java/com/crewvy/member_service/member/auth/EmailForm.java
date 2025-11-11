package com.crewvy.member_service.member.auth;

public class EmailForm {
    public static String generateForm(String newPassword) {
        // html 문법 적용한 메일의 내용
        return String.format("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>비밀번호 초기화</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f4f7f6; font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', '맑은 고딕', sans-serif;">
                
                    <div style="width: 100%%; max-width: 600px; margin: 40px auto; background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden;">
                
                        <div style="background-color: #0056b3; color: #ffffff; padding: 30px 40px;">
                            <h1 style="margin: 0; font-size: 26px; font-weight: 600;">비밀번호 초기화</h1>
                        </div>
                
                        <div style="padding: 40px;">
                
                            <p style="margin-top: 0; font-size: 16px; color: #333; line-height: 1.6;">
                                안녕하세요.<br>
                                요청하신 임시 비밀번호가 발급되었습니다.
                            </p>
                
                            <div style="background-color: #f9f9f9; border: 1px dashed #cccccc; border-radius: 6px; padding: 25px; margin: 30px 0; text-align: center;">
                                <p style="margin: 0; font-size: 16px; color: #555;">새로운 임시 비밀번호:</p>
                                <strong style="display: block; margin-top: 10px; font-size: 28px; color: #0056b3; letter-spacing: 2px; font-weight: 700;">
                                    %s
                                </strong>
                            </div>
                
                            <p style="font-size: 16px; color: #333; line-height: 1.6;">
                                보안을 위해, 로그인 후 <strong style="color: #D93025;">즉시 비밀번호를 변경</strong>해 주시기 바랍니다.
                            </p>
                
                            <div style="text-align: center; margin-top: 35px; margin-bottom: 20px;">
                                <a href="%s"
                                   style="display: inline-block; background-color: #007bff; color: #ffffff; font-size: 16px; font-weight: 600; text-decoration: none; padding: 14px 28px; border-radius: 5px;">
                                    로그인하러 가기
                                </a>
                            </div>
                
                        </div>
                
                        <div style="background-color: #f9f9f9; color: #888888; padding: 30px 40px; text-align: center; border-top: 1px solid #e0e0e0;">
                            <p style="margin: 0; font-size: 12px;">
                                본 메일은 발신 전용입니다.
                            </p>
                            <p style="margin: 5px 0 0; font-size: 12px;">
                                &copy; 2025 [H.ONE]. All rights reserved.
                            </p>
                        </div>
                
                    </div>
                
                </body>
                </html>
                """, newPassword, "https://hone.crewvy.cloud");
    }
}
