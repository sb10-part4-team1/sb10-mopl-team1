package com.sb10.mopl.config;

import com.sb10.mopl.user.admin.AdminAccountProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminAccountProperties.class)
public class AdminAccountConfig {}
