package org.testng.reporters;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.internal.Utils;
import org.testng.internal.annotations.Sets;
import org.testng.xml.XmlSuite;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class JUnitReportReporter implements IReporter {

  @Override
  public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
      String defaultOutputDirectory) {

    String outputDirectory = defaultOutputDirectory + File.separator + "junitreports";
    Map<Class<?>, Set<ITestResult>> results = Maps.newHashMap();
    Map<Class<?>, Set<ITestResult>> failedConfigurations = Maps.newHashMap();
    for (ISuite suite : suites) {
      Map<String, ISuiteResult> suiteResults = suite.getResults();
      for (ISuiteResult sr : suiteResults.values()) {
        ITestContext tc = sr.getTestContext();
        addResults(tc.getPassedTests().getAllResults(), results);
        addResults(tc.getFailedTests().getAllResults(), results);
        addResults(tc.getSkippedTests().getAllResults(), results);
        addResults(tc.getFailedConfigurations().getAllResults(), failedConfigurations);
      }
    }

    for (Map.Entry<Class<?>, Set<ITestResult>> entry : results.entrySet()) {
      Class<?> cls = entry.getKey();
      Properties p1 = new Properties();
      p1.setProperty("name", cls.getName());
      Date timeStamp = Calendar.getInstance().getTime();
      p1.setProperty(XMLConstants.ATTR_TIMESTAMP, timeStamp.toGMTString());

      List<TestTag> testCases = Lists.newArrayList();
      int failures = 0;
      int errors = 0;
      int testCount = 0;
      float totalTime = 0;

      for (ITestResult tr: entry.getValue()) {
        TestTag testTag = new TestTag();

        boolean isSuccess = tr.getStatus() == ITestResult.SUCCESS;
        if (! isSuccess) {
          if (tr.getThrowable() instanceof AssertionError) {
            errors++;
          } else {
            failures++;
          }
        }

        Properties p2 = new Properties();
        p2.setProperty("classname", cls.getName());
        p2.setProperty("name", tr.getMethod().getMethodName());
        long time = tr.getEndMillis() - tr.getStartMillis();
        p2.setProperty("time", "" + formatTime(time));
        Throwable t = getThrowable(tr, failedConfigurations);
        if (! isSuccess && t != null) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          t.printStackTrace(pw);
          testTag.message = t.getMessage();
          testTag.type = t.getClass().getName();
          testTag.stackTrace = sw.toString();
          testTag.errorTag = tr.getThrowable() instanceof AssertionError ? "error" : "failure";
        }
        totalTime += time;
        testCount++;
        testTag.properties = p2;
        testCases.add(testTag);
      }

      p1.setProperty("failures", "" + failures);
      p1.setProperty("errors", "" + errors);
      p1.setProperty("name", cls.getName());
      p1.setProperty("tests", "" + testCount);
      p1.setProperty("time", "" + formatTime(totalTime));
      try {
        p1.setProperty(XMLConstants.ATTR_HOSTNAME, InetAddress.getLocalHost().getHostName());
      } catch (UnknownHostException e) {
        // ignore
      }

      //
      // Now that we have all the information we need, generate the file
      //
      XMLStringBuffer xsb = new XMLStringBuffer();
      xsb.addComment("Generated by " + getClass().getName());

      xsb.push("testsuite", p1);
      for (TestTag testTag : testCases) {
        if (testTag.stackTrace == null) {
          xsb.addEmptyElement("testcase", testTag.properties);
        }
        else {
          xsb.push("testcase", testTag.properties);

          Properties p = new Properties();
          if (testTag.message != null) {
            p.setProperty("message", testTag.message);
          }
          p.setProperty("type", testTag.type);
          xsb.push(testTag.errorTag, p);
          xsb.addCDATA(testTag.stackTrace);
          xsb.pop(testTag.errorTag);

          xsb.pop("testcase");
        }
      }
      xsb.pop("testsuite");

      String fileName = "TEST-" + cls.getName() + ".xml";
      Utils.writeFile(outputDirectory, fileName, xsb.toXML());
    }

//    System.out.println(xsb.toXML());
//    System.out.println("");

  }

  private String formatTime(float time) {
    DecimalFormat format = new DecimalFormat("#.###");
    format.setMinimumFractionDigits(3);
    return format.format(time / 1000.0f);
  }

  private Throwable getThrowable(ITestResult tr,
      Map<Class<?>, Set<ITestResult>> failedConfigurations) {
    Throwable result = tr.getThrowable();
    if (result == null && tr.getStatus() == ITestResult.SKIP) {
      // Attempt to grab the stack trace from the configuration failure
      for (Set<ITestResult> failures : failedConfigurations.values()) {
        for (ITestResult failure : failures) {
          // Naive implementation for now, eventually, we need to try to find
          // out if it's this failure that caused the skip since (maybe by
          // seeing if the class of the configuration method is assignable to
          // the class of the test method, although that's not 100% fool proof
          if (failure.getThrowable() != null) {
            return failure.getThrowable();
          }
        }
      }
    }

    return result;
  }

  class TestTag {
    public Properties properties;
    public String message;
    public String type;
    public String stackTrace;
    public String errorTag;
  }

  private void addResults(Set<ITestResult> allResults, Map<Class<?>, Set<ITestResult>> out) {
    for (ITestResult tr : allResults) {
      Class<?> cls = tr.getMethod().getTestClass().getRealClass();
      Set<ITestResult> l = out.get(cls);
      if (l == null) {
        l = Sets.newHashSet();
        out.put(cls, l);
      }
      l.add(tr);
    }
  }

}
