package com.friskysoft.framework;

import io.github.bonigarcia.wdm.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Browser implements WebDriver {

    private static ThreadLocal<WebDriver> wrappedThreadLocalDriver = new ThreadLocal<>();
    private static Browser singletonBrowser;
    private static String defaultScreenshotDir = "./screenshots";

    public static final int DEFAULT_IMPLICIT_WAIT = 5;
    public static final int DEFAULT_EXPLICIT_WAIT = 10;

    public static final String CHROMEDRIVER_SYSTEM_PROPERTY = "webdriver.chrome.driver";
    public static final String GECKODRIVER_SYSTEM_PROPERTY = "webdriver.gecko.driver";

    private static final Log LOGGER = LogFactory.getLog(Browser.class);

    public static WebDriver getWebDriver() {
        return wrappedThreadLocalDriver.get();
    }

    public static Browser setWebDriver(WebDriver driver) {
        wrappedThreadLocalDriver.set(driver);
        return singletonBrowser;
    }

    public static Browser setup(WebDriver driver) {
        setWebDriver(driver);
        return singletonBrowser;
    }

    /**
     * Use newInstance() methods instead
     */
    private Browser() {}

    public static Browser newInstance() {
        if (singletonBrowser == null) {
            singletonBrowser = new Browser();
        }
        return singletonBrowser;
    }

    @SuppressWarnings("deprecation")
    public static Browser newInstance(String browserType) {
        WebDriver driver;
        DesiredCapabilities capabilities = getDefaultBrowserCapabilities(browserType);
        switch (browserType) {
            case BrowserType.CHROME:
                ChromeDriverManager.getInstance().setup();
                driver = new ChromeDriver(capabilities);
                break;
            case BrowserType.FIREFOX:
                FirefoxDriverManager.getInstance().setup();
                driver = new FirefoxDriver(capabilities);
                break;
            case BrowserType.SAFARI:
                driver = new SafariDriver(capabilities);
                break;
            case BrowserType.OPERA:
            case BrowserType.OPERA_BLINK:
                OperaDriverManager.getInstance().setup();
                driver = new OperaDriver(capabilities);
                break;
            case BrowserType.IE:
            case BrowserType.IEXPLORE:
                InternetExplorerDriverManager.getInstance().setup();
                driver = new InternetExplorerDriver(capabilities);
                break;
            case BrowserType.EDGE:
                EdgeDriverManager.getInstance().setup();
                driver = new EdgeDriver(capabilities);
                break;
            case BrowserType.HTMLUNIT:
            case BrowserType.PHANTOMJS:
            default:
                ChromeDriverManager.getInstance().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--headless", "--disable-gpu");
                driver = new ChromeDriver(chromeOptions);
                break;
        }
        setWebDriver(driver);
        return newInstance().fullscreen();
    }

    public static Browser newInstance(WebDriver driver) {
        setWebDriver(driver);
        return newInstance();
    }

    public static Browser newRemoteInstance(String remoteHubUrl, String browserType) {
        URL url; 
        try {
            url = new URL(remoteHubUrl);
        } catch (MalformedURLException ex) {
            throw new AssertionError("Invalid remote hub url: " + remoteHubUrl);
        }
        WebDriver remoteDriver = new RemoteWebDriver(url, getDefaultBrowserCapabilities(browserType));
        setWebDriver(remoteDriver);
        return newInstance();
    }

    public static Browser newRemoteInstance(URL remoteHubUrl, String browserType) {
        WebDriver remoteDriver = new RemoteWebDriver(remoteHubUrl, getDefaultBrowserCapabilities(browserType));
        setWebDriver(remoteDriver);
        return newInstance();
    }

    private static URL getResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    @SuppressWarnings("deprecation")
    public static DesiredCapabilities getDefaultBrowserCapabilities(String browserType) {
        switch (browserType) {
            case BrowserType.CHROME:
                return DesiredCapabilities.chrome();
            case BrowserType.FIREFOX:
                return DesiredCapabilities.firefox();
            case BrowserType.SAFARI:
                return DesiredCapabilities.safari();
            case BrowserType.OPERA:
            case BrowserType.OPERA_BLINK:
                return DesiredCapabilities.operaBlink();
            case BrowserType.IE:
            case BrowserType.IEXPLORE:
                return DesiredCapabilities.internetExplorer();
            case BrowserType.HTMLUNIT:
            case BrowserType.PHANTOMJS:
            default:
                return DesiredCapabilities.chrome();
        }
    }

    public enum ArchType {
        X86(32), X64(64);

        ArchType(int value) {
            this.value = value;
        }

        int value;

        public int getValue() {
            return value;
        }
    }

    public enum PlatformType {
        WINDOWS, MAC, LINUX
    }

    private static PlatformType getPlatformType() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return PlatformType.MAC;
        } else if (os.contains("linux")) {
            return PlatformType.LINUX;
        } else if (os.contains("win")) {
            return PlatformType.WINDOWS;
        } else {
            return PlatformType.LINUX;
        }
    }

    private static ArchType getArchType() {
        boolean is64bit;
        if (System.getProperty("os.name").contains("Windows")) {
            is64bit = (System.getenv("ProgramFiles(x86)") != null);
        } else {
            is64bit = (System.getProperty("os.arch").contains("64"));
        }
        return is64bit ? ArchType.X64 : ArchType.X86;
    }

    @Override
    public void get(String url) {
        LOGGER.info("Opening page at url: " + url);
        getWebDriver().get(url);
    }

    @Override
    public String getCurrentUrl() {
        return getWebDriver().getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return getWebDriver().getTitle();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return getWebDriver().findElements(by);
    }

    @Override
    public WebElement findElement(By by) {
        return getWebDriver().findElement(by);
    }

    @Override
    public String getPageSource() {
        return getWebDriver().getPageSource();
    }

    @Override
    public void close() {
        getWebDriver().close();
    }

    @Override
    public void quit() {
        getWebDriver().quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return getWebDriver().getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return getWebDriver().getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return getWebDriver().switchTo();
    }

    @Override
    public Navigation navigate() {
        return getWebDriver().navigate();
    }

    @Override
    public Options manage() {
        return getWebDriver().manage();
    }

    public Browser fullscreen() {
        try {
            getWebDriver().manage().window().fullscreen();
        } catch (Exception ex1) {
            try {
                int w = Integer.parseInt(executeScript("return screen.width").toString());
                int h = Integer.parseInt(executeScript("return screen.height").toString());
                resize(w, h);
            } catch (Exception ex2) {
                LOGGER.warn(String.format("Fullscreen failed with errors <%s> and <%s> ", ex1.getMessage(), ex2.getMessage()));
            }
        }
        return this;
    }

    public Browser resize(int width, int height) {
        try {
            getWebDriver().manage().window().setPosition(new Point(0, 0));
            getWebDriver().manage().window().setSize(new Dimension(width, height));
        } catch (Exception ex) {
            LOGGER.warn(String.format("Resize failed with error <%s>", ex.getMessage()));
        }
        return this;
    }

    public Actions getActions() {
        return new Actions(getWebDriver());
    }

    public JavascriptExecutor getJavascriptExecutor() {
        return (JavascriptExecutor)(getWebDriver());
    }

    public Object executeScript(String script, Object... args) {
        return getJavascriptExecutor().executeScript(script, args);
    }

    public Object executeAsyncScript(String script, Object... args) {
        return getJavascriptExecutor().executeAsyncScript(script, args);
    }

    public Object injectJQuery() {
        return injectJQuery("3.0.0");
    }

    public Object injectJQuery(String version) {
        return executeAsyncScript(String.format(Utilities.JQUERY_LOADER_SCRIPT, version));
    }

    public void destroy() {
        try {
            getWebDriver().close();
        } catch (Exception ignore) {}
        try {
            getWebDriver().quit();
        } catch (Exception ignore) {}
        setWebDriver(null);
    }

    public Browser open(String url) {
        this.get(url);
        return this;
    }

    public Browser sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            // ignore
        }
        return this;
    }

    public Browser wait(int time, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(time));
        } catch (InterruptedException ex) {
            // ignore
        }
        return this;
    }

    public Browser setDefaultScreenshotDir(String defaultScreenshotDir) {
        Browser.defaultScreenshotDir = defaultScreenshotDir;
        return this;
    }

    public String takeScreenshot() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String methodName = stackTraceElements[2].getMethodName();
        String className = stackTraceElements[2].getClassName();
        String[] classNameSplit = className.split("\\.");
        className = classNameSplit[classNameSplit.length-1];

        DateFormat format = new SimpleDateFormat("YYYYMMdd_HHmmss");
        String title = getTitle().replaceAll("[^A-Za-z0-9]", "_");
        return takeScreenshot(String.format(defaultScreenshotDir + "/screenshot_%s_%s_%s_%s.png",
                format.format(new Date()), className, methodName, title));
    }

    public String takeScreenshot(String filepath) {
        try {
            File scrFile = ((TakesScreenshot) getWebDriver()).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(scrFile, new File(filepath));
            return new File(filepath).getAbsolutePath();
        } catch (IOException ioex) {
            ioex.printStackTrace();
            return null;
        }
    }

    public Browser setPageLoadTimeout(int time, TimeUnit unit) {
        getWebDriver().manage().timeouts().pageLoadTimeout(time, unit);
        return this;
    }

    public Browser setScriptTimeout(int time, TimeUnit unit) {
        getWebDriver().manage().timeouts().setScriptTimeout(time, unit);
        return this;
    }

    public Browser setImplicitWait(int time, TimeUnit unit) {
        getWebDriver().manage().timeouts().implicitlyWait(time, unit);
        return this;
    }

    public Browser waitForElementToBePresent(By by) {
        return waitForElementToBePresent(by, DEFAULT_EXPLICIT_WAIT);
    }

    public Browser waitForElementToBePresent(By by, int timeOutInSeconds) {
        new WebDriverWait(getWebDriver(), timeOutInSeconds).until(ExpectedConditions.presenceOfElementLocated(by));
        return this;
    }

    public Browser waitForElementToBeClickable(By by) {
        return waitForElementToBeClickable(by, DEFAULT_EXPLICIT_WAIT);
    }

    public Browser waitForElementToBeClickable(By by, int timeOutInSeconds) {
        new WebDriverWait(getWebDriver(), timeOutInSeconds).until(ExpectedConditions.elementToBeClickable(by));
        return this;
    }

}