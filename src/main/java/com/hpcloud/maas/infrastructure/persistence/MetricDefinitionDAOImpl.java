package com.hpcloud.maas.infrastructure.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import com.hpcloud.maas.common.model.metric.MetricDefinition;
import com.hpcloud.maas.domain.service.MetricDefinitionDAO;

/**
 * MetricDefinition DAO implementation.
 * 
 * @author Jonathan Halterman
 */
public class MetricDefinitionDAOImpl implements MetricDefinitionDAO {
  private static final String METRIC_DEF_SQL = "select sa.namespace, sa.metric_type, sa.metric_subject, sad.dimensions from sub_alarm as sa, "
      + "(select sub_alarm_id, group_concat(dimension_name, '=', value) as dimensions from sub_alarm_dimension group by sub_alarm_id) as sad "
      + "where sa.id = sad.sub_alarm_id";

  private final DBI db;

  @Inject
  public MetricDefinitionDAOImpl(DBI db) {
    this.db = db;
  }

  @Override
  public List<MetricDefinition> findForAlarms() {
    Handle h = db.open();

    try {
      List<Map<String, Object>> rows = h.createQuery(METRIC_DEF_SQL).list();

      List<MetricDefinition> metricDefs = new ArrayList<MetricDefinition>(rows.size());
      for (Map<String, Object> row : rows) {
        String namespace = (String) row.get("namespace");
        String type = (String) row.get("metric_type");
        String subject = (String) row.get("metric_subject");
        String dimensionSet = (String) row.get("dimensions");
        Map<String, String> dimensions = null;

        if (dimensionSet != null) {
          dimensions = new HashMap<String, String>();
          for (String kvStr : dimensionSet.split(",")) {
            String[] kv = kvStr.split("=");
            if (kv.length > 1)
              dimensions.put(kv[0], kv[1]);
          }
        }

        metricDefs.add(new MetricDefinition(namespace, type, subject, dimensions));
      }

      return metricDefs;
    } finally {
      h.close();
    }
  }
}
