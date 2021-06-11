package functions;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Utils {

    public static List<String> tokenize(String input, String delimiter, boolean required) {
        List<String> output = new ArrayList<>();

        if(input.isBlank() && required){
            throw new IllegalArgumentException(String.format(
                    "Input string '%s' is blank.",
                    input
            ));
        }

        if(input.isBlank() && !required){
            return output;
        }

        StringTokenizer tokens = new StringTokenizer(input, delimiter);
        while (tokens.hasMoreTokens()) {
            output.add(tokens.nextToken().trim());
        }
        if (required && output.size() == 0) {
            throw new IllegalArgumentException(String.format(
                    "No tokens found in string: '%s' using delimiter '%s'",
                    input,
                    delimiter
            ));
        }
        return output;
    }

    public static String getConfigFromEnv(String config, boolean required){
        String value = System.getenv().getOrDefault(config, "");

        if(required && value.isBlank()){
            throw new IllegalArgumentException(String.format("Missing environment variable '%s'",config));
        }

        return value;
    }
}
