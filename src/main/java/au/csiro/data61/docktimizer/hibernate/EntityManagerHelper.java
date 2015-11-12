package au.csiro.data61.docktimizer.hibernate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 */
public class EntityManagerHelper {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(EntityManagerHelper.class);

    private static EntityManagerHelper instance;
    private EntityManager entityManager;

    public static EntityManagerHelper getInstance() {
        if (instance == null) {
            instance = new EntityManagerHelper();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        Properties properties = new Properties();


        Properties prop = new Properties();
        try {

            String mysqlPropertyFile = System.getenv("MYSQL_PROPERTY_FILE");
            if (mysqlPropertyFile != null) {
                prop.load(new FileInputStream(mysqlPropertyFile));
            } else {

                prop.load(instance.getClass().getClassLoader().
                        getResourceAsStream("mysql-config/mysql.properties"));
            }

            String mysqlTcpAddr = prop.getProperty("MYSQL_TCP_ADDR");
            if (mysqlTcpAddr.startsWith("$")) {
                mysqlTcpAddr = System.getenv(mysqlTcpAddr.replace("$", ""));
            }
            String mysqlTcpPort = prop.getProperty("MYSQL_TCP_PORT");
            if (mysqlTcpPort.startsWith("$")) {
                mysqlTcpPort = System.getenv(mysqlTcpPort.replace("$", ""));
            }
            String mysqlUserName = prop.getProperty("MYSQL_USER_NAME");
            String mysqlUserPassword = prop.getProperty("MYSQL_USER_PASSWORD");
            String mysqlDatabaseName = prop.getProperty("MYSQL_DATABASE_NAME");


            LOG.info(mysqlTcpAddr);
            LOG.info(mysqlTcpPort);
            LOG.info(mysqlUserName);
            LOG.info(mysqlUserPassword);
            LOG.info(mysqlDatabaseName);

            String value = "jdbc:mysql://" + mysqlTcpAddr + ":" +
                    mysqlTcpPort + "/" + mysqlDatabaseName;
            LOG.info(value);

            properties.put("javax.persistence.jdbc.url", value);
            properties.put("javax.persistence.jdbc.user", mysqlUserName);
            properties.put("javax.persistence.jdbc.password", mysqlUserPassword);

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("dockerPU", properties);
            entityManager = emf.createEntityManager();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized EntityManager getEntityManager() {
        return entityManager;
    }
}
