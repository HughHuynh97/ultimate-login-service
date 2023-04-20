package com.tool.sqlpad.controller;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("aqs")
public class AqsLoginController {

    static {
        WebDriverManager.chromedriver().setup();
    }

   // private static final ChromeDriver DRIVER = getChromeDriver();

//    public static ChromeDriver getChromeDriver() {
//        ChromeOptions chromeOptions = new ChromeOptions();
//        chromeOptions.addArguments("--start-maximized");
//        chromeOptions.addArguments("--headless");
//        chromeOptions.addArguments("--remote-allow-origins=*");
//        return new ChromeDriver(chromeOptions);
//    }

    @PostMapping("test")
    public Object login() {
/*        DRIVER.get("https://bggqat.beatus88.com/");
        var nav = DRIVER.findElement(By.tagName("ul"));
        nav.findElements(By.tagName("li")).get(3).click();*/
        return "";
    }
}
