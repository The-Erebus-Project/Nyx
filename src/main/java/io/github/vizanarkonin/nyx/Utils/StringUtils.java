package io.github.vizanarkonin.nyx.Utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    private static final Logger logger = LogManager.getLogger(StringUtils.class.getName());

    public static String generateRandomStringWithNumbers(int length) {
        return RandomStringUtils.random(length, true, true);
    }

    public static String getRegexGroupValue(String source, String regex, int groupNumberToReturn) {
        Matcher matcher = Pattern.compile(regex).matcher(source);
        if (matcher.find())
            return matcher.group(groupNumberToReturn);
        else
            return "";
    }
}

