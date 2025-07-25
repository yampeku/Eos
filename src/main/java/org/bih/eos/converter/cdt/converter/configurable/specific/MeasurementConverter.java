package org.bih.eos.converter.cdt.converter.configurable.specific;

import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.DvCount;
import com.nedap.archie.rm.datavalues.quantity.DvOrdinal;
import com.nedap.archie.rm.datavalues.quantity.DvProportion;
import com.nedap.archie.rm.datavalues.quantity.DvQuantity;
import org.bih.eos.converter.cdm_field.concept.OperatorConverter;
import org.bih.eos.converter.cdm_field.numeric.RangeHighConverter;
import org.bih.eos.converter.cdm_field.numeric.RangeLowConverter;
import org.bih.eos.converter.cdt.converter.configurable.DvGetter;
import org.bih.eos.converter.cdt.converter.configurable.generic.CDTValueUnitConverter;
import org.bih.eos.converter.cdm_field.concept.DVTextCodeToConceptConverter;
import org.bih.eos.converter.cdt.DefaultConverterServices;
import org.bih.eos.converter.cdt.conversion_entities.MeasurementEntity;
import org.bih.eos.converter.PathProcessor;
import org.bih.eos.yaml.cdt_configs.measurement.MeasurementConfig;
import org.bih.eos.yaml.ValueEntry;
import org.bih.eos.converter.dao.ConvertableContentItem;
import org.bih.eos.jpabase.entity.JPABaseEntity;
import org.bih.eos.jpabase.entity.Concept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class MeasurementConverter extends CDTValueUnitConverter<MeasurementEntity, MeasurementConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementConverter.class);
    private final OperatorConverter operatorConverter;
    private final RangeHighConverter rangeHighConverter = new RangeHighConverter();
    private final RangeLowConverter rangeLowConverter = new RangeLowConverter();

    public MeasurementConverter(DefaultConverterServices defaultConverterServices, MeasurementConfig measurementConfig) {
        super(defaultConverterServices, measurementConfig);
        this.operatorConverter = new OperatorConverter(defaultConverterServices);
    }

    @Override
    public Optional<JPABaseEntity> convertInternal(ConvertableContentItem convertableContentItem) {
        MeasurementConfig measurementConfig = (MeasurementConfig) omopMapping;
        MeasurementEntity measurement = new MeasurementEntity(measurementConfig, convertableContentItem.getPerson(), defaultConverterServices.getConceptService().findById(32817L));
        measurement = convertRequiredFields(convertableContentItem, measurementConfig, measurement);
        measurement = convertOptionalFields(convertableContentItem.getContentItem(), measurementConfig, measurement);
        return measurement.toJpaEntity();
    }

    @Override
    protected MeasurementEntity convertRequiredFields(ConvertableContentItem convertableContentItem, MeasurementConfig measurementConfig, MeasurementEntity measurement) {
        Locatable contentItem = convertableContentItem.getContentItem();
        convertConceptCode(contentItem, measurementConfig.getConceptId().getAlternatives(), measurement);
        convertDateTime(contentItem, measurementConfig.getMeasurementDate().getAlternatives(), measurement);
        getVisitOccurrence().ifPresent(measurement::setVisitOccurrence);
        return measurement;
    }

    @Override
    protected MeasurementEntity convertOptionalFields(Locatable contentItem, MeasurementConfig measurementConfig, MeasurementEntity measurement) {
        if (measurementConfig.getValue().isPopulated()) {
            measurement = convertValue(contentItem, measurementConfig.getValue().getAlternatives(), measurement);
        }
        if (measurementConfig.getUnit().isPopulated()) {
            convertUnitCode(contentItem, measurementConfig.getUnit().getAlternatives(), measurement);
        }
        if (measurementConfig.getVisitDetailId().isPopulated()) {
            unsupportedMapping("visit_detail_id");
        }
        if (measurementConfig.getProviderId().isPopulated()) {
            unsupportedMapping("provider_id");
        }
        if (measurementConfig.getMeasurementEventId().isPopulated()) {
            unsupportedMapping("measurement_event_id");
        }
        if (measurementConfig.getMeasEventFieldConceptId().isPopulated()) {
            unsupportedMapping("meas_event_field_concept_id");
        }
        if (measurementConfig.getOperatorConceptId().isPopulated()) {
            measurement.setOperator(convertOperator(contentItem, measurementConfig.getOperatorConceptId().getAlternatives()));
        }
        if (measurementConfig.getRangeLow().isPopulated()) {
            measurement.setRangeLow(convertRangeLow(contentItem, measurementConfig.getRangeLow().getAlternatives()));
        }
        if (measurementConfig.getRangeHigh().isPopulated()) {
            measurement.setRangeHigh(convertRangeHigh(contentItem, measurementConfig.getRangeHigh().getAlternatives()));
        }
        return measurement;
    }



    private void convertDateTime(Locatable contentItem, ValueEntry[] dateTime, MeasurementEntity measurement) {
        Optional<Date> date = dateTimeConverter.convert(contentItem, dateTime);
        measurement.setDateTime(date);
    }

    private void convertUnitCode(Locatable contentItem, ValueEntry[] valueEntries, MeasurementEntity measurement) {
        measurement.setUnitConcept( getUnitStandardConverter().convert(contentItem, valueEntries),
                 getUnitSourceConceptConverter().convert(contentItem, valueEntries),
                getUnitSourceValueConverter().convert(contentItem, valueEntries));
    }

    @Override
    protected MeasurementEntity convertValuePath(Locatable contentItem, String path, MeasurementEntity measurement) {
        Optional<?> valueItem = PathProcessor.getItemAtPath(contentItem, path);
        DVTextCodeToConceptConverter conceptConverter = defaultConverterServices.getElementToConceptConverter();
        if (valueItem.isPresent()) {
            Element element = (Element) valueItem.get();
            return convertDataValue(measurement, element, conceptConverter);
        }
        return measurement;
    }

    private MeasurementEntity convertDataValue(MeasurementEntity measurement, Element element, DVTextCodeToConceptConverter conceptConverter) {
        Object value = element.getValue();
        if (value == null) {
            return measurement;
        }

        if (value instanceof DvQuantity) {
            handleDvQuantity(measurement, (DvQuantity) value);
        } else if (value instanceof DvText) {
            handleDvText(measurement, (DvText) value);
        } else if (value instanceof DvCodedText) {
            handleDvCodedText(measurement, (DvCodedText) value, conceptConverter);
        } else if (value instanceof DvOrdinal) {
            setDvOrdinal((DvOrdinal) value, measurement, conceptConverter);
        } else if (value instanceof DvProportion) {
            setDvProportion((DvProportion) value, measurement, conceptConverter);
        } else if (value instanceof DvCount) {
            handleDvCount(measurement, (DvCount) value);
        }

        return measurement;
    }
    
    private void handleDvQuantity(MeasurementEntity measurement, DvQuantity value) {
        measurement.setValue(DvGetter.getDvQuantity(value));
    }

    private void handleDvText(MeasurementEntity measurement, DvText value) {
        Optional<String> dvText = DvGetter.getDvText(value);
        if (dvText.isPresent() && dvText.get().length() >= 50) {
            String text = dvText.get();
            LOG.warn("Text exceeds 50 characters and will not fit on textual values, the mapping will be ignored (if not optional). Text value: {}", text);
            dvText = Optional.empty();
        }
        measurement.setValue(dvText);
    }

    private void handleDvCodedText(MeasurementEntity measurement, DvCodedText value, DVTextCodeToConceptConverter conceptConverter) {
        Optional<Concept> concept = conceptConverter.convertStandardConcept(value);
        measurement.setValue(concept, DvGetter.getDvCodedText(value));
    }

    private void handleDvCount(MeasurementEntity measurement, DvCount value) {
        measurement.setValue(DvGetter.getDvCount(value));
    }

    private void setDvProportion(DvProportion dvProportion, MeasurementEntity measurement, DVTextCodeToConceptConverter conceptConverter) {
        if (dvProportion.getType() == 2) {
            measurement.setValue(OptionalDouble.of(dvProportion.getNumerator()));//numerator is 1..1 cant be null
        } else {
            LOG.warn("Proportion types besides 2 are not supported, the mapping will be ignored (if not optional)");
        }
    }

    private void setDvOrdinal(DvOrdinal dvOrdinal, MeasurementEntity measurement, DVTextCodeToConceptConverter conceptConverter) {
        if (dvOrdinal.getSymbol() != null) {
            Element dvOrdinalValueAsElement = new Element();
            dvOrdinalValueAsElement.setValue(dvOrdinal.getSymbol());
            convertDataValue(measurement, dvOrdinalValueAsElement, conceptConverter);
        } else {
            if (dvOrdinal.getValue() != null) {
                measurement.setValue(Optional.of(dvOrdinal.getValue().toString()));
            } else {
                measurement.setValue(Optional.empty());
            }
        }
    }

    @Override
    protected MeasurementEntity convertValueCode(Long code, MeasurementEntity measurement) {
        Concept concept = defaultConverterServices.getConceptService().findById(code);
        if (concept != null) {
            measurement.setValue(Optional.of(concept), Optional.of(code + ""));
        } else {
            measurement.setValue(Optional.empty(), Optional.of(code + ""));
        }
        LOG.info("Currently there is no way initialising a value to the value concept, so only the concept is set now without a magnitude. It is better to provide this information within the composition than setting it manually");
        return measurement;
    }

    @Override
    protected MeasurementEntity convertValueCodeConceptMap(Long code, MeasurementEntity measurement) {
        Concept concept = new Concept(code);
        if (code != null) {
            measurement.setValue(Optional.of(concept), Optional.of(code + ""));
        } else {
            measurement.setValue(Optional.empty(), Optional.of(code + ""));
        }
        LOG.info("Currently there is no way initialising a value to the value concept, so only the concept is set now without a magnitude. It is better to provide this information within the composition than setting it manually");
        return measurement;
    }

    private Optional<Concept> convertOperator(Locatable contentItem, ValueEntry[] alternatives) {
        return operatorConverter.convert(contentItem, alternatives);
    }

    private Optional<Double> convertRangeLow(Locatable contentItem, ValueEntry[] alternatives) {
        return rangeLowConverter.convert(contentItem, alternatives);
    }

    private Optional<Double> convertRangeHigh(Locatable contentItem, ValueEntry[] alternatives) {
        return rangeHighConverter.convert(contentItem, alternatives);
    }



}
