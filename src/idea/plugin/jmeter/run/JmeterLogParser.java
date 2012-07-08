package idea.plugin.jmeter.run;

import com.google.common.base.Preconditions;
import idea.plugin.jmeter.domain.Assertion;
import idea.plugin.jmeter.domain.SampleResult;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringEscapeUtils.unescapeXml;

public class JmeterLogParser {

    private enum State {inTestResults, inSample, inAssertion;}

    private final JmeterConsoleView console;

    private State state = State.inTestResults;
    private String sampleName;
    private String samplerData;
    private String responseData;
    private Assertion assertion;
    private List<Assertion> assertions;

    public JmeterLogParser(JmeterConsoleView console) {
        this.console = console;
    }


    public void parseLine(String line) {
        System.out.println(line);
        String trim = line.trim();
        if (state == State.inTestResults && (trim.startsWith("<sample") || trim.startsWith("<httpSample"))) {
            state = State.inSample;
            sampleName = extractAttribute(line, "lb");
            samplerData = null;
            responseData = null;
            assertions = new ArrayList<Assertion>();
            assertion = new Assertion();

            if (trim.endsWith("/>")) {
                state = State.inTestResults;
                printSampleResult();
            }
        }

        if (state == State.inSample && line.contains("<assertionResult>")) {
            state = State.inAssertion;
        }

        if (state == State.inSample && line.contains("<samplerData")) {
            samplerData = extractTagBody(line, "samplerData");
        }

        if (state == State.inSample && line.contains("<responseData")) {
            responseData = extractTagBody(line, "responseData");
        }

        if (state == State.inAssertion && line.contains("<name>")) {
            assertion.setName(extractAssertionName(line));
        }

        if (state == State.inAssertion && line.contains("<failure>")) {
            assertion.setFailure(extractAssertionFailure(line));
        }

        if (state == State.inAssertion && line.contains("<failureMessage>")) {
            assertion.setFailureMessage(extractTagBody(line, "failureMessage"));
        }

        if (state == State.inAssertion && line.contains("<error>")) {
            assertion.setError(extractAssertionError(line));
        }

        if (state == State.inAssertion && line.contains("</assertionResult>")) {
            state = State.inSample;
            assertions.add(assertion);
        }

        if (state == State.inSample && (line.contains("</sample>") || line.contains("</httpSample>"))) {
            state = State.inTestResults;
            printSampleResult();
        }
    }

    private void printSampleResult() {
        SampleResult sampleResult = new SampleResult(sampleName, samplerData, responseData, assertions);
        console.addSampleResult(sampleResult);
    }

    private String extractAttribute(String line, String name) {
        int start = line.indexOf(name + "=\"") + (name + "=\"").length();
        int end = line.indexOf('"', start + 1);
        return unescapeXml(line.substring(start, end));
    }

    private String extractAssertionName(String line) {
        return extractTagBody(line, "name");
    }

    private boolean extractAssertionFailure(String line) {
        return Boolean.valueOf(extractTagBody(line, "failure"));
    }

    private boolean extractAssertionError(String line) {
        return Boolean.valueOf(extractTagBody(line, "error"));
    }

    private String extractTagBody(String line, String name) {
        String tag = "<" + name;
        int start = line.indexOf(tag) + tag.length();
        Preconditions.checkState(start >= 0);
        start = line.indexOf('>', start);
        Preconditions.checkState(start > 0);
        if (line.charAt(start - 1) == '/') {
            return "";
        }
        int end = line.indexOf("</" + name + ">", start);
        return unescapeXml(line.substring(start + 1, end));
    }

/*
        writer.addAttribute("t", Long.toString(res.getTime()));
        writer.addAttribute("it", Long.toString(res.getIdleTime()));
        writer.addAttribute("lt", Long.toString(res.getLatency()));
        writer.addAttribute("ts", Long.toString(res.getTimeStamp()));
        writer.addAttribute("s", Boolean.toString(res.isSuccessful()));
        writer.addAttribute("lb", ConversionHelp.encode(res.getSampleLabel()));
        writer.addAttribute("rc", ConversionHelp.encode(res.getResponseCode()));
        writer.addAttribute("rm", ConversionHelp.encode(res.getResponseMessage()));
        writer.addAttribute("tn", ConversionHelp.encode(res.getThreadName()));
        writer.addAttribute("dt", ConversionHelp.encode(res.getDataType()));
        writer.addAttribute("de", ConversionHelp.encode(res.getDataEncodingNoDefault()));
        writer.addAttribute("by", String.valueOf(res.getBytes()));
        writer.addAttribute("sc", String.valueOf(res.getSampleCount()));
        writer.addAttribute("ec", String.valueOf(res.getErrorCount()));
        writer.addAttribute("ng", String.valueOf(res.getGroupThreads()));
        writer.addAttribute("na", String.valueOf(res.getAllThreads()));
        writer.addAttribute("hn", event.getHostname());

        for (int i = 0; i < SampleEvent.getVarCount(); i++){
            writer.addAttribute(SampleEvent.getVarName(i), ConversionHelp.encode(event.getVarValue(i)));
        }
    }
*/


}