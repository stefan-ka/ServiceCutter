package ch.hsr.servicestoolkit.score.relations;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ch.hsr.servicestoolkit.model.MonoCouplingInstance;
import ch.hsr.servicestoolkit.score.cuts.CouplingCriterionScoring;

public class LifecycleCriterionScorer {

	public Map<FieldTuple, Double> getScores(final Set<MonoCouplingInstance> instances) {
		Map<FieldTuple, Double> result = new HashMap<>();
		for (MonoCouplingInstance instance : instances) {
			for (int i = 0; i < instance.getDataFields().size() - 1; i++) {
				for (int j = i + 1; j < instance.getDataFields().size(); j++) {
					result.put(new FieldTuple(instance.getDataFields().get(i), instance.getDataFields().get(j)), CouplingCriterionScoring.MAX_SCORE);
				}
			}
		}
		return result;
	}
}