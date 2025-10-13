package tetoandeggens.seeyouagainbe.auth.util;

import java.util.Random;

public class GeneratorRandomUtil {
    private static final Random random = new Random();

    public static String generateRandomNum() {
        int num = random.nextInt(900000) + 100000;
        return String.valueOf(num);
    }
//
//    private GeneratorRandomUtil() {
//        throw new UnsupportedOperationException("This is a utility class");
//    }
}
