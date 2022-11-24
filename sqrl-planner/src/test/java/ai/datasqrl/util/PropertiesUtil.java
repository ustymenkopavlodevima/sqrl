package ai.datasqrl.util;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

  public static Properties properties = PropertiesUtil.getProperties("local.properties");

  public static Properties getProperties(String name) {
    Properties prop = new Properties();
    try (InputStream resourceAsStream =
        PropertiesUtil.class.getClassLoader().getResourceAsStream(name)) {
      prop.load(resourceAsStream);
    } catch (Exception ignored) {
    }
    return prop;
  }
}