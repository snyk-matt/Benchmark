/**
 * OWASP Benchmark Project
 *
 * <p>This file is part of the Open Web Application Security Project (OWASP) Benchmark Project For
 * details, please see <a
 * href="https://owasp.org/www-project-benchmark/">https://owasp.org/www-project-benchmark/</a>.
 *
 * <p>The OWASP Benchmark is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, version 2.
 *
 * <p>The OWASP Benchmark is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details
 *
 * @author Dave Wichers
 * @created 2015
 */
package org.owasp.benchmark.score.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.owasp.benchmark.score.BenchmarkScore;

public class ContrastReader extends Reader {

    public static void main(String[] args) throws Exception {
        File f = new File("results/Benchmark_1.2-Contrast.log");
        ContrastReader cr = new ContrastReader();
        cr.parse(f);
    }

    public TestResults parse(File f) throws Exception {
        TestResults tr = new TestResults("Contrast", true, TestResults.ToolType.IAST);

        BufferedReader reader = new BufferedReader(new FileReader(f));
        String FIRSTLINEINDICATOR =
                BenchmarkScore.TESTCASENAME
                        + StringUtils.repeat("0", BenchmarkScore.TESTIDLENGTH - 1)
                        + "1";
        String firstLine = null;
        String lastLine = "";
        String line = "";
        while (line != null) {
            try {
                line = reader.readLine();
                if (line != null) {
                    if (line.startsWith("{\"hash\":")) {
                        parseContrastFinding(tr, line);
                    } else if (line.contains("Agent Version:")) {
                        String version =
                                line.substring(line.indexOf("Version:") + "Version:".length());
                        tr.setToolVersion(version.trim());
                    } else if (line.contains("DEBUG - >>> [URL")
                            && line.contains(FIRSTLINEINDICATOR)) {
                        firstLine = line;
                    } else if (line.contains("DEBUG - >>> [URL")) {
                        lastLine = line;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        reader.close();
        tr.setTime(calculateTime(firstLine, lastLine));
        return tr;
    }

    private void parseContrastFinding(TestResults tr, String json) throws Exception {
        TestCaseResult tcr = new TestCaseResult();

        try {
            JSONObject obj = new JSONObject(json);
            String ruleId = obj.getString("ruleId");
            tcr.setCWE(cweLookup(ruleId));
            tcr.setCategory(ruleId);

            JSONObject request = obj.getJSONObject("request");
            String uri = request.getString("uri");

            if (uri.contains(BenchmarkScore.TESTCASENAME)) {
                String testNumber =
                        uri.substring(
                                uri.lastIndexOf('/') + BenchmarkScore.TESTCASENAME.length() + 1);
                tcr.setNumber(Integer.parseInt(testNumber));
                if (tcr.getCWE() != 0) {
                    // System.out.println( tcr.getNumber() + "\t" + tcr.getCWE() + "\t" +
                    // tcr.getCategory() );
                    tr.put(tcr);
                }
            }
        } catch (Exception e) {
            // There are a few crypto-bad-mac & crypto-weak-randomness findings not associated with
            // a request, so ignore errors associated with those.
            if (!json.contains("\"ruleId\":\"crypto-bad-mac\"")
                    && !json.contains("\"ruleId\":\"crypto-weak-randomness\"")) {
                System.err.println("Contrast Results Parse error for: " + json);
                e.printStackTrace();
            }
        }
    }

    private static int cweLookup(String rule) {
        switch (rule) {
            case "cmd-injection":
                return 78; // command injection
            case "cookie-flags-missing":
                return 614; // insecure cookie use
            case "crypto-bad-ciphers":
                return 327; // weak encryption
            case "crypto-bad-mac":
                return 328; // weak hash
            case "crypto-weak-randomness":
                return 330; // weak random
            case "csp-header-insecure":
                return 0000; // Don't care
            case "csp-header-missing":
                return 0000; // Don't care
            case "header-injection":
                return 113; // header injection
            case "hql-injection":
                return 564; // hql injection
            case "hsts-header-missing":
                return 319; // CWE-319: Cleartext Transmission of Sensitive Information
            case "ldap-injection":
                return 90; // ldap injection
            case "path-traversal":
                return 22; // path traversal
            case "reflected-xss":
                return 79; // xss
            case "reflection-injection":
                return 0000; // reflection injection
            case "redos":
                return 400; // regex denial of service - CWE-400: Uncontrolled Resource Consumption
            case "sql-injection":
                return 89; // sql injection
            case "trust-boundary-violation":
                return 501; // trust boundary
            case "unsafe-readline":
                return 0000; // unsafe readline
            case "xcontenttype-header-missing":
                return 0000; // Don't care
            case "xpath-injection":
                return 643; // xpath injection
            case "xxe":
                return 611; // xml entity
            default:
                System.out.println("WARNING: Contrast-Unrecognized finding type: " + rule);
        }

        return 0;
    }

    private String calculateTime(String firstLine, String lastLine) {
        try {
            String start = firstLine.split(" ")[1];
            String stop = lastLine.split(" ")[1];
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss,SSS");
            Date startTime = sdf.parse(start);
            Date stopTime = sdf.parse(stop);
            long startMillis = startTime.getTime();
            long stopMillis = stopTime.getTime();
            long seconds = (stopMillis - startMillis) / 1000;
            return seconds + " seconds";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

