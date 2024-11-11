package de.checkerce.utils;

import org.random.api.RandomOrgClient;
import java.io.IOException;

public class AtmosphericRandom {
    static final String API_KEY = "4061b7e3-bdcd-4fbe-9fd9-056f69dac64b";

    static RandomOrgClient roc = RandomOrgClient.getRandomOrgClient(API_KEY);

    public static double nextDouble(){
        try {
            return roc.generateDecimalFractions(1, 2)[0];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
