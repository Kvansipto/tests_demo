package framework

import org.openqa.selenium.By
import org.openqa.selenium.By.xpath

fun byText(text: String): By = xpath(".//*[text()='$text']")

fun byClass(value: String): By = xpath(".//*[contains(concat(' ', @class, ' '), ' $value ')]")