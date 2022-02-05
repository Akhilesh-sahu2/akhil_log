package com.akhil.java.loganalyser.service;

import com.akhil.java.loganalyser.conf.ApplicationData;
import com.akhil.java.loganalyser.manager.LogAnalyserManager;
import com.akhil.java.loganalyser.model.Context;
import com.akhil.java.loganalyser.validator.LogAnalyserValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogAnalyserService {

    @Autowired
    private LogAnalyserValidator validator;

    @Autowired
    private LogAnalyserManager manager;

    public void execute(String... args) {
        Context context = Context.getInstance();
        validator.validateInput(context, args);
        manager.parseAndPersistEvents(context);
    }

}
