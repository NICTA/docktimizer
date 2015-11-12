package au.csiro.data61.docktimizer.testClient;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Parser {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(Parser.class);
    private static String resultStarting1QuadCoreWith3Deployed_BaseLine = "TODO: Replace this with a result string";


    public static void main(String[] args) {
        String[] split = resultStarting1QuadCoreWith3Deployed_BaseLine.split("\n");
        StringBuilder builder = new StringBuilder("");
        builder.append("Round")
                .append(";").append("Time")
                .append(";").append("Total Cores")
                .append(";").append("Unused Cores")
                .append(";").append("App 1")
                .append(";").append("App 2")
                .append(";").append("App 3")
                .append("\n");
        long start = 0;
        {
            int app0 = 0;
            int app1 = 80;
            int total = 0;
            for (int i = 0; i < 30; i++) {
                app0 += 20;
                if ((i < 30 / 2)) {
                    app1 += 5;
                } else {
                    app1 -= 5;
                }

                int app2 = 50;
                total += app1 + app0 + app2;
            }
            LOG.info("" + total);
        }

        for (String s : split) {


            int single = StringUtils.countMatches(s, "_1");
            int duale = StringUtils.countMatches(s, "_2") * 2;
            int quad = StringUtils.countMatches(s, "_4") * 4;
            int hexa = StringUtils.countMatches(s, "_8") * 8;

            int app1 = 0;
            int app2 = 0;
            int app3 = 0;
            for (int i = 0; i < 3; i++) {
                int tm = 0;
                tm += StringUtils.countMatches(s, "app" + i + "-1");
                tm += StringUtils.countMatches(s, "app" + i + "-2") * 2;
                tm += StringUtils.countMatches(s, "app" + i + "-4") * 4;
                tm += StringUtils.countMatches(s, "app" + i + "-8") * 8;
                if (i == 0) {
                    app1 += tm;
                }
                if (i == 1) {
                    app2 += tm;
                }
                if (i == 2) {
                    app3 += tm;
                }
            }

            int totalCores = single + duale + quad + hexa;
            int free = totalCores - app1 - app2 - app3;
            int i = s.indexOf(";");
            String round = s.substring(0, i);
            String replace = s.substring(i + 1);
            int i1 = replace.indexOf(";");
            Long time = Long.parseLong(replace.substring(0, i1));
            if (start == 0) {
                start = time;
            }
            builder.append(round)
                    .append(";").append((time - start) / 1000)
                    .append(";").append(totalCores)
                    .append(";").append(free)
                    .append(";").append(app1)
                    .append(";").append(app2)
                    .append(";").append(app3).append("\n");
        }
        LOG.info(builder.toString());

    }


}
