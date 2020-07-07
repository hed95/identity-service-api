package io.digital.patterns.identity.service;


import io.digital.patterns.identity.service.model.MrzScan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MrzService {

    public List<MrzScan> getScans(String correlationId) {
        return new ArrayList<>();
    }

    public void create(MrzScan mrzScan) {

    }
}
