package me.bayang.reader.storage;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.dmfs.httpessentials.exceptions.ProtocolException;
import org.dmfs.oauth2.client.OAuth2AccessToken;
import org.dmfs.oauth2.client.OAuth2Scope;
import org.dmfs.oauth2.client.scope.StringScope;
import org.dmfs.rfc5545.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import me.bayang.reader.config.AppConfig;
import me.bayang.reader.config.AppConfigurationDecoder;
import me.bayang.reader.rssmodels.UserInformation;
import me.bayang.reader.share.wallabag.WallabagCredentials;
import me.bayang.reader.utils.Theme;

@Service
@Primary
public class FileConfigStorageServiceImpl implements IStorageService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigStorageServiceImpl.class);
    
    @Value("${app.css:LIGHT}")
    private String appCss;
    
    private Theme appTheme;
    
    @Resource(name="userConfig")
    private FileBasedConfigurationBuilder<FileBasedConfiguration> userConfig;
    
    @Resource(name="tokenConfig")
    private FileBasedConfigurationBuilder<FileBasedConfiguration> tokenConfig;
    
    @Resource(name="applicationConfig")
    private FileBasedConfigurationBuilder<FileBasedConfiguration> appConfig;
    
    @Autowired
    private AppConfigurationDecoder configurationDecoder;
    
    private Configuration appConfiguration;
    private Configuration userConfiguration;
    private Configuration tokenConfiguration;
    
    private BooleanProperty pocketEnabled = new SimpleBooleanProperty();
    private BooleanProperty wallabagEnabled = new SimpleBooleanProperty();
    private BooleanProperty prefersGridLayout = new SimpleBooleanProperty();
    private StringProperty pocketUser = new SimpleStringProperty();
    
    
    @PostConstruct
    public void initialize() throws IOException, ConfigurationException {
        appTheme = Theme.valueOf(appCss);
        
        if (! AppConfig.appConfigDir.exists()) {
            boolean dirCreated = AppConfig.appConfigDir.mkdirs();
            if (! dirCreated) {
                LOGGER.error("app config dir could not be created '{}'", AppConfig.appConfigDir.toString());
            }
        }
        
        File propertiesFile = new File(AppConfig.appConfigDir,"config.properties");
        if (! propertiesFile.exists()) {
            propertiesFile.createNewFile();
        }
        File tokenPropertiesFile = new File(AppConfig.appConfigDir,"token.properties");
        if (! tokenPropertiesFile.exists()) {
            tokenPropertiesFile.createNewFile();
        }
        File userPropertiesFile = new File(AppConfig.appConfigDir,"user.properties");
        if (! userPropertiesFile.exists()) {
            userPropertiesFile.createNewFile();
        }
        this.appConfiguration = appConfig.getConfiguration();
        this.userConfiguration = userConfig.getConfiguration();
        this.tokenConfiguration = tokenConfig.getConfiguration();
        
        prefersGridLayoutProperty().set(appConfiguration.getBoolean("prefers.layout.grid", false));
        prefersGridLayoutProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                LOGGER.debug("prefers grid layout {} ",newValue);
                setPrefersGridLayout(newValue);
            }
        });
        pocketEnabledProperty().set(appConfiguration.getBoolean("pocket.enabled", false));
        pocketEnabledProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                LOGGER.debug("pocket enabled {} ",newValue);
                setPocketEnabled(newValue);
            }
        });
        pocketUserProperty().set(loadPocketUser());
        pocketUserProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                LOGGER.debug("pocket user {} ",newValue);
                setPocketUser(newValue);
            }
        });
        wallabagEnabledProperty().set(appConfiguration.getBoolean("wallabag.enabled", false));
        wallabagEnabledProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                LOGGER.debug("wallabag enabled {} ",newValue);
                setWallabagEnabled(newValue);
            }
        });
    }

    @Override
    public void saveToken(OAuth2AccessToken token) throws Exception {
        String accessToken = String.valueOf(token.accessToken());
        String refreshToken = String.valueOf(token.refreshToken());
        String scope = token.scope().toString();
        LOGGER.debug("access {}, refresh {}, scope {}", accessToken, refreshToken, scope);
        
        tokenConfiguration.setProperty("inoreader.token.access", accessToken);
        tokenConfiguration.setProperty("inoreader.token.refresh", refreshToken);
        tokenConfiguration.setProperty("inoreader.token.scope", scope);
    }

    @Override
    public OAuth2AccessToken loadToken() {
        String refresh = tokenConfiguration.getString("inoreader.token.refresh");
        String scope = tokenConfiguration.getString("inoreader.token.scope");
        OAuth2AccessToken token = new OAuth2AccessToken() {
            public CharSequence accessToken() throws ProtocolException {
                throw new UnsupportedOperationException("accessToken not present");
            }

            public CharSequence tokenType() throws ProtocolException {
                throw new UnsupportedOperationException("tokenType not present");
            }

            public boolean hasRefreshToken() {
               return true;
            }

            public CharSequence refreshToken() throws ProtocolException {
               return refresh;
            }

            public DateTime expirationDate() throws ProtocolException {
                throw new UnsupportedOperationException("expirationDate not present");
            }

            public OAuth2Scope scope() throws ProtocolException {
                return new StringScope(scope);
            }
        };
        return token;  
    }

    @Override
    public void saveUser(UserInformation user) {
        userConfiguration.setProperty("inoreader.userId", user.getUserId());
    }

    @Override
    public String loadUser() {
        return userConfiguration.getString("inoreader.userId");
    }
    
    public String getPocketUser() {
        return pocketUserProperty().get();
    }
    
    @Override
    public void setPocketUser(String user) {
        pocketUserProperty().set(user);
        savePocketUser(user);
    }
    
    @Override
    public StringProperty pocketUserProperty() {
        return pocketUser;
    }
    
    @Override
    public BooleanProperty pocketEnabledProperty() {
        return pocketEnabled;
    }
    
    public boolean isPocketEnabled() {
        return pocketEnabledProperty().get();
    }
    
    public void setPocketEnabled(boolean enabled) {
        pocketEnabledProperty().set(enabled);
        savePocketEnabled(enabled);
    }
    
    public void savePocketEnabled(boolean enabled) {
        appConfiguration.setProperty("pocket.enabled", enabled);
        LOGGER.debug("saving pocket enabled state : {}", enabled);
    }
    
    @Override
    public BooleanProperty wallabagEnabledProperty() {
        return wallabagEnabled;
    }
    
    public boolean isWallabagEnabled() {
        return wallabagEnabledProperty().get();
    }
    
    public void setWallabagEnabled(boolean enabled) {
        wallabagEnabledProperty().set(enabled);
        saveWallabagEnabled(enabled);
    }
    
    public void saveWallabagEnabled(boolean enabled) {
        appConfiguration.setProperty("wallabag.enabled", enabled);
        LOGGER.debug("saving wallabag enabled state : {}", enabled);
    }
    
    @Override
    public WallabagCredentials loadWallabagCredentials() {
        String url = userConfiguration.getString("wallabag.url", "");
        String clientId = userConfiguration.getString("wallabag.clientId", "");
        String clientSecret = userConfiguration.getString("wallabag.clientSecret", "");
        String refreshToken = tokenConfiguration.getString("wallabag.refreshToken", "");
        
        return new WallabagCredentials(url, null, null, clientId, clientSecret, refreshToken, null);
    }
    
    @Override
    public void saveWallabagCredentials(WallabagCredentials wallabagCredentials) {
        LOGGER.debug(wallabagCredentials.toString());
        if (! StringUtils.isBlank(wallabagCredentials.getUrl())) {
            userConfiguration.setProperty("wallabag.url", wallabagCredentials.getUrl());
        }
        if (! StringUtils.isBlank(wallabagCredentials.getClientId())) {
            userConfiguration.setProperty("wallabag.clientId", wallabagCredentials.getClientId());
        }
        if (! StringUtils.isBlank(wallabagCredentials.getClientSecret())) {
            userConfiguration.setProperty("wallabag.clientSecret", wallabagCredentials.getClientSecret());
        }
        if (! StringUtils.isBlank(wallabagCredentials.getRefreshToken())) {
            tokenConfiguration.setProperty("wallabag.refreshToken", wallabagCredentials.getRefreshToken());
        }
    }
    
    @Override
    public void saveWallabagRefreshToken(String token) {
        if (! StringUtils.isBlank(token)) {
            tokenConfiguration.setProperty("wallabag.refreshToken", token);
        }
    }

    @Override
    public void savePocketToken(String token) {
        tokenConfiguration.setProperty("pocket.token", token);
    }

    @Override
    public String loadPocketToken() {
        return tokenConfiguration.getString("pocket.token", "");
    }

    public void savePocketUser(String user) {
        userConfiguration.setProperty("pocket.user", user);
    }
    
    public String loadPocketUser() {
        return userConfiguration.getString("pocket.user", "");
    }

    @Override
    public boolean hasToken() {
        return (tokenConfiguration != null 
                && tokenConfiguration.getString("inoreader.token.refresh") != null 
                && tokenConfiguration.getString("inoreader.token.scope") != null);
    }

    @Override
    public boolean hasUser() {
        return (userConfiguration != null && userConfiguration.getString("inoreader.userId") != null);
    }
    
    @Override
    public BooleanProperty prefersGridLayoutProperty() {
        return prefersGridLayout;
    }
    
    @Override
    public boolean prefersGridLayout() {
        return prefersGridLayoutProperty().get();
    }
    
    public void setPrefersGridLayout(boolean prefersGridLayout) {
        prefersGridLayoutProperty().set(prefersGridLayout);
        appConfiguration.setProperty("prefers.layout.grid", prefersGridLayout);
        LOGGER.debug("saving prefers grid layout state : {}", prefersGridLayout);
    }

    public Theme getAppTheme() {
        return appTheme;
    }

    public void setAppTheme(Theme appTheme) {
        this.appTheme = appTheme;
        appConfiguration.setProperty("app.css", appTheme.name());
    }
    

}
