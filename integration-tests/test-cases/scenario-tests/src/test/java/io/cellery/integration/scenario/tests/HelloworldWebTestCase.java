/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.cellery.integration.scenario.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Paths;

import static io.github.bonigarcia.wdm.DriverManagerType.CHROME;

/**
 * This includes the test cases related to hello world web scenario.
 */
public class HelloworldWebTestCase extends BaseTestCase {
    private final String imageName = "hello-world-web";
    private final String version = "1.0.0";
    private final String helloWorldInstance = "hello-world-inst";
    private WebDriver webDriver;

    @BeforeClass
    public void setup() {
        WebDriverManager.getInstance(CHROME).setup();
        webDriver = new ChromeDriver(new ChromeOptions().setHeadless(true));
    }

    @Test(groups = "build")
    public void build() throws Exception {
        build("hello-world.bal", Constants.TEST_CELL_ORG_NAME, imageName, version,
                Paths.get(CELLERY_SCENARIO_TEST_ROOT, "hello-world-web").toFile().getAbsolutePath());
    }

    @Test(dependsOnGroups = "build")
    public void run() throws Exception {
        run(Constants.TEST_CELL_ORG_NAME, imageName, version, helloWorldInstance, 120);
    }

    @Test(dependsOnMethods = "run")
    public void invoke() {
        webDriver.get(Constants.DEFAULT_HELLO_WORLD_URL);
        validateWebPage();
    }

    @Test(dependsOnMethods = "run")
    public void terminate() throws Exception {
        terminateCell(helloWorldInstance);
    }

    @Test(dependsOnMethods = "terminate", expectedExceptions = Exception.class)
    public void repeatTerminate() throws Exception {
        terminateCell(helloWorldInstance);
    }

    private void validateWebPage() {
        String searchHeader = webDriver.findElement(By.cssSelector("H1")).getText().toLowerCase();
        Assert.assertEquals(searchHeader, Constants.HELLO_WORLD_WEB_CONTENT, "Web page is content is not as expected");
    }

    @AfterClass
    public void cleanup() {
        webDriver.close();
        try {
            terminateCell(helloWorldInstance);
        } catch (Exception ignored) {
        }
    }
}
