package org.bih.eos.converter.cdt.converter.configurable.specific;

import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.DvCount;
import com.nedap.archie.rm.datavalues.quantity.DvQuantity;
import org.bih.eos.converter.PathProcessor;
import org.bih.eos.converter.cdt.DefaultConverterServices;
import org.bih.eos.converter.cdt.conversion_entities.ObservationEntity;
import org.bih.eos.converter.cdt.converter.configurable.DvGetter;
import org.bih.eos.converter.cdt.converter.configurable.generic.CDTValueUnitConverter;
import org.bih.eos.converter.dao.ConvertableContentItem;
import org.bih.eos.jpabase.entity.Concept;
import org.bih.eos.jpabase.entity.JPABaseEntity;
import org.bih.eos.yaml.ValueEntry;
import org.bih.eos.yaml.cdt_configs.observation.ObservationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Optional;

public class ObservationConverter extends CDTValueUnitConverter<ObservationEntity, ObservationConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(ObservationConverter.class);

    public ObservationConverter(DefaultConverterServices defaultConverterServices, ObservationConfig observationConfig) {
        super(defaultConverterServices, observationConfig);
    }

    @Override
    public Optional<JPABaseEntity> convertInternal(ConvertableContentItem convertableContentItem) {
        ObservationConfig observationConfig = (ObservationConfig) omopMapping;
        ObservationEntity observation = new ObservationEntity(observationConfig, convertableContentItem.getPerson(), defaultConverterServices.getConceptService().findById(32817L));
        observation = convertRequiredFields(convertableContentItem, observationConfig, observation);
        observation = convertOptionalFields(convertableContentItem.getContentItem(), observationConfig, observation);
        return observation.toJpaEntity();
    }

    @Override
    protected ObservationEntity convertRequiredFields(ConvertableContentItem convertableContentItem, ObservationConfig observationConfig, ObservationEntity observation) {
        Locatable contentItem = convertableContentItem.getContentItem();
        convertDateTime(contentItem, observationConfig.getObservationDate().getAlternatives(), observation);
        convertConceptCode(contentItem, observationConfig.getConceptId().getAlternatives(), observation);
        getVisitOccurrence().ifPresent(observation::setVisitOccurrence);
        return observation;
    }

    @Override
    protected ObservationEntity convertOptionalFields(Locatable contentItem, ObservationConfig observationConfig, ObservationEntity observation) {
        if (observationConfig.getValue().isPopulated()) {
            observation = convertValue(contentItem, observationConfig.getValue().getAlternatives(), observation);
        }
        if (observationConfig.getUnit().isPopulated()) {
            convertUnitCode(contentItem, observationConfig.getUnit().getAlternatives(), observation);
        }
        if (observationConfig.getQualifier().isPopulated()) {
            convertQualifier(contentItem, observationConfig.getQualifier().getAlternatives(), observation);
        }
        if (observationConfig.getProviderId().isPopulated()) {
            unsupportedMapping("provider_id");
        }
        if (observationConfig.getVisitDetailId().isPopulated()) {
            unsupportedMapping("visit_detail_id");
        }
        if (observationConfig.getObservationEventId().isPopulated()) {
            unsupportedMapping("observation_event_id");
        }
        if (observationConfig.getObsEventFieldConceptId().isPopulated()) {
            unsupportedMapping("obs_event_field_concept_id");
        }
        return observation;
    }

    private void convertQualifier(Locatable contentItem, ValueEntry[] alternatives, ObservationEntity observation) {
        observation.setQualifier( standardConceptConverter.convert(contentItem, alternatives),
                sourceValueConverter.convert(contentItem, alternatives));
    }

    private void convertDateTime(Locatable contentItem, ValueEntry[] observationDate, ObservationEntity observation) {
        observation.setDateTime((Optional<Date>) dateTimeConverter.convert(contentItem, observationDate));
    }

    //TODO Unit duplicate, value too but only to some degree
    private void convertUnitCode(Locatable contentItem, ValueEntry[] valueEntries, ObservationEntity observation) {
        observation.setUnitConcept(getUnitStandardConverter().convert(contentItem, valueEntries));
        observation.setUnitSourceValue(getUnitSourceValueConverter().convert(contentItem, valueEntries));
    }

    @Override
    protected ObservationEntity convertValueCode(Long code, ObservationEntity observation) {
        Concept concept = defaultConverterServices.getConceptService().findById(code);
        if (concept != null) {
            observation.setValue(Optional.of(concept), Optional.of(code + ""));
        } else {
            observation.setValue(Optional.empty(), Optional.of(code + ""));
        }
        LOG.info("Currently there is no way initialising a value to the value concept, so only the concept is set now without a magnitude. It is better to provide this information within the composition then setting it manually");
        return observation;
    }
    
    @Override
    protected ObservationEntity convertValueCodeConceptMap(Long code, ObservationEntity observation) {
    	Concept concept = new Concept(code);
        if (code != null) {
            observation.setValue(Optional.of(concept), Optional.of(code + ""));
        } else {
            observation.setValue(Optional.empty(), Optional.of(code + ""));
        }
        LOG.info("Currently there is no way initialising a value to the value concept, so only the concept is set now without a magnitude. It is better to provide this information within the composition then setting it manually");
        return observation;
    }

    @Override
    protected ObservationEntity convertValuePath(Locatable contentItem, String path, ObservationEntity observation) {
        Optional<?> valueItem = PathProcessor.getItemAtPath(contentItem, path);
        if (valueItem.isPresent()) {
            Element element = (Element) valueItem.get();
            return convertDataValues(observation, element);
        }
        return observation;
    }

    private ObservationEntity convertDataValues(ObservationEntity observation, Element element) {
        Object value = element.getValue();
        if (value == null) {
            return observation;
        }

        if (value instanceof DvQuantity) {
            handleDvQuantity(observation, (DvQuantity) value);
        } else if (value instanceof DvText) {
            handleDvText(observation, (DvText) value);
        } else if (value instanceof DvCodedText) {
            handleDvCodedText(observation, (DvCodedText) value);
        } else if (value instanceof DvCount) {
            handleDvCount(observation, (DvCount) value);
        }

        return observation;
    }

    private void handleDvQuantity(ObservationEntity observation, DvQuantity value) {
        observation.setValue(DvGetter.getDvQuantity(value));
    }

    private void handleDvText(ObservationEntity observation, DvText value) {
        Optional<String> dvText = DvGetter.getDvText(value);
        if (dvText.isPresent() && dvText.get().length() >= 50) {
            String text = dvText.get();
            LOG.warn("Text exceeds 50 characters and will not fit on textual values, the mapping will be ignored (if not optional). Text value: {}", text);
            dvText = Optional.empty();
        }
        observation.setValue(dvText);
    }

    private void handleDvCodedText(ObservationEntity observation, DvCodedText value) {
        Optional<Concept> concept = defaultConverterServices.getElementToConceptConverter().convertStandardConcept(value);
        observation.setValue(concept, DvGetter.getDvCodedText(value));
    }

    private void handleDvCount(ObservationEntity observation, DvCount value) {
        observation.setValue(DvGetter.getDvCount(value));
    }


}