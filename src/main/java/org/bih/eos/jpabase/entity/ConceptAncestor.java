/*******************************************************************************
 * Copyright (c) 2019 Georgia Tech Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *

 * Copyright (c) 2023 Berlin Institute of Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/

package org.bih.eos.jpabase.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import net.jcip.annotations.Immutable;

@Entity
@Immutable
@Table(name="concept_ancestor")
@Inheritance(strategy=InheritanceType.JOINED)
public class ConceptAncestor extends JPABaseEntity implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@ManyToOne
	@JoinColumn(name="ancestor_concept_id")
	private Concept ancestorConcept;

	@ManyToOne
	@JoinColumn(name="descendant_concept_id")
	private Concept descendantConcept;

	@Column(name="min_levels_of_separation")
	private Integer minLevelsOfSeparation;
	
	@Column(name="max_levels_of_separation")
	private Integer maxLevelsOfSeparation;
	
	public Concept getAncestorConcept() {
		return this.ancestorConcept;
	}
	
	public Concept getDescendantConcept() {
		return this.descendantConcept;
	}
	
	public Integer getMinLevelsOfSeparation() {
		return this.minLevelsOfSeparation;
	}
	
	public Integer getMaxLevelsOfSeparation() {
		return this.maxLevelsOfSeparation;
	}
	
	@Override
	public Long getIdAsLong() {
		return null;
	}
	
	
	@Override
	public boolean equals(Object object) {
	    if (this == object) return true;
	    if (object == null || getClass() != object.getClass()) return false;
	    ConceptAncestor that = (ConceptAncestor) object;
	    return Objects.equals(ancestorConcept, that.ancestorConcept) &&
	           Objects.equals(descendantConcept, that.descendantConcept);
	}

	@Override
	public int hashCode() {
	    return Objects.hash(ancestorConcept, descendantConcept);
	}

}
