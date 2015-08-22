package com.skcraft.plume.common.service.journal;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.util.ProfileService;

/**
 * Created by Boris Lachev on 8/22/2015.
 */
public class CriteriaParser{
    public Criteria parse(String input)throws UsedCommaException{
        Criteria.Builder builder = new Criteria.Builder();
        String[] tokens = input.split(" ");
        String parsed;
        String token;
        for(int i = 0; i < tokens.length; i++){
            token = tokens[i];
            if(!token.contains(",")) {
                if (token.length() >= 3 && token.charAt(1) == ':') {
                    switch (Character.toLowerCase(token.charAt(0))) {
                        case 'w':
                            parsed = token.substring(2);
                            builder.setWorldName(parsed);
                            break;
                    }
                } else if (token.length() >= 3 && token.charAt(1) != ':') {
                    switch (token) {
                        case "world":
                            parsed = tokens[i + 1];
                            builder.setWorldName(parsed);
                            break;
                        case "player":
                            parsed = tokens[i + 1];
                            //builder.setUserId();
                    }
                }
            } else {
                throw new UsedCommaException("You cannot use a comma in the input.");
            }
        }
        return builder.build();
    }
    public static class UsedCommaException extends Exception {
        public UsedCommaException(){}

        public UsedCommaException(String message){
            super(message);
        }
    }
}
