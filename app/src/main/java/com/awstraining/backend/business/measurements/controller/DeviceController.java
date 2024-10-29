package com.awstraining.backend.business.measurements.controller;

import static java.lang.System.currentTimeMillis;

import java.util.List;

import com.awstraining.backend.api.rest.v1.DeviceIdApi;
import com.awstraining.backend.api.rest.v1.model.Measurement;
import com.awstraining.backend.api.rest.v1.model.Measurements;
import com.awstraining.backend.business.measurements.MeasurementDO;
import com.awstraining.backend.business.measurements.MeasurementService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("device/v1")
class DeviceController implements DeviceIdApi {
    private static final Logger LOGGER = LogManager.getLogger(DeviceController.class);

    private final MeasurementService service;

    private final MeterRegistry mReg;

    @Autowired
    public DeviceController(final MeasurementService service, MeterRegistry registry) {
        this.service = service;
        this.mReg = registry;
    }

    @Override
    public ResponseEntity<Measurement> publishMeasurements(final String deviceId, final Measurement measurement) {
        LOGGER.info("Publishing measurement for device '{}'", deviceId);
        mReg.counter("publishMeasurements.counter", "method", "publishMeasurements").increment();
        final MeasurementDO measurementDO = fromMeasurement(deviceId, measurement);
        service.saveMeasurement(measurementDO);
        return ResponseEntity.ok(measurement);
    }
    @Override
    public ResponseEntity<Measurements> retrieveMeasurements(final String deviceId) {
        LOGGER.info("Retrieving all measurements for device '{}'", deviceId);
        mReg.counter("retrieveMeasurements.counter", "method", "retrieveMeasurements").increment();
        final List<Measurement> measurements = service.getMeasurements()
                .stream()
                .map(this::toMeasurement)
                .toList();
        LOGGER.info("Measurements size '{}'", measurements.size());
        final Measurements measurementsResult = new Measurements();
        measurementsResult.measurements(measurements);
        return ResponseEntity.ok(measurementsResult);
    }

    private Measurement toMeasurement(final MeasurementDO measurementDO) {
        final Measurement measurement = new Measurement();
        measurement.setTimestamp(measurementDO.getCreationTime());
        measurement.setType(measurementDO.getType());
        measurement.setValue(measurementDO.getValue());
        return measurement;
    }

    private MeasurementDO fromMeasurement(final String deviceId, final Measurement measurement) {
        final MeasurementDO measurementDO = new MeasurementDO();
        measurementDO.setDeviceId(deviceId);
        measurementDO.setType(measurement.getType());
        measurementDO.setValue(measurement.getValue());
        final Long creationTime = measurement.getTimestamp();
        measurementDO.setCreationTime(creationTime == null ? currentTimeMillis() : creationTime);
        return measurementDO;
    }
}
