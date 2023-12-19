package org.tckry.shortlink.admin.toolkit;

import java.security.SecureRandom;

/**
 * 分组ID随机生成器
 * @author: lydms
 * @create: 2023-12-19 15:25
 **/
public final class RandomGenerator {
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxy";
    private static final SecureRandom RANDOM = new SecureRandom();


    /**
     * 生成随机分组ID
     * @return: java.lang.String　分组id
     * @Date: 2023/12/19
     */

    public static String generateRandomString() {
        return generateRandomString(6);
    }


    /** 
    * 生成随机分组ID
    * @Param: [length]　生成多少位
    * @return: java.lang.String　
    * @Date: 2023/12/19
    */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}
