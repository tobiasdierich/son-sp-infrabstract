provider "kubernetes" {
    host = "{{ endpoint }}"

    token = "{{ token }}"
    cluster_ca_certificate = "{{ caCert }}"
}

{% for vdu in csd.getVirtualDeploymentUnits() %}
resource "kubernetes_replication_controller" "{{ vdu.getName() }}" {
  metadata {
    name = "{{ vdu.getName() }}"
    labels {
      app = "{{ vdu.getId() }}"
    }
  }

  spec {
    selector {
      app = "{{ vdu.getId() }}"
    }
    template {
      container {
        image = "{{ vdu.getServiceImage() }}"
        name  = "{{ vdu.getName() }}"

        {% for port in vdu.getServicePorts() %}
        port {
            {% if port.getName() != null %}
            name = "{{ port.getName() }}"
            {% endif %}
            {% if port.getProtocol() != null %}
            protocol = "{{ port.getProtocol() }}"
            {% endif %}
            container_port = {{ port.getTargetPort() }}
        }
        {% endfor %}

        {% if vdu.getResourceRequirements() != null %}
            resources {
              requests {
                {% if vdu.getResourceRequirements().getCpu() != null %}
                cpu = {{ vdu.getResourceRequirements().getCpu().getvCpus() }}
                {% endif %}

                {% if vdu.getResourceRequirements().getMemory() != null %}
                memory = "{{ vdu.getResourceRequirements().getMemory().getSize() }}{{ vdu.getResourceRequirements().getMemory().getSizeUnit() }}"
                {% endif %}
              }
            }
        {% endif %}
      }
    }
  }
}

resource "kubernetes_service" "{{ vdu.getName() }}" {
  metadata {
    name = "{{ vdu.getName() }}"
  }
  spec {
    selector {
      app = "${kubernetes_replication_controller.{{ vdu.getName() }}.metadata.0.labels.app}"
    }

    {% for port in vdu.getServicePorts() %}
    port {
        {% if port.getName() != null %}
        name = "{{ port.getName() }}"
        {% endif %}
        {% if port.getProtocol() != null %}
        protocol = "{{ port.getProtocol() }}"
        {% endif %}
        port = {{ port.getPort() }}
        target_port = {{ port.getTargetPort() }}
    }
    {% endfor %}

    type = "LoadBalancer"
  }
}

resource "kubernetes_horizontal_pod_autoscaler" "{{ vdu.getName() }}" {
  metadata {
    name = "{{ vdu.getName() }}"
  }
  spec {
    max_replicas = {{ vdu.getScalingConfiguration().getMaximum() }}
    min_replicas = {{ vdu.getScalingConfiguration().getMinimum() }}
    scale_target_ref {
      kind = "ReplicationController"
      name = "${kubernetes_replication_controller.{{ vdu.getName() }}.metadata.0.name}"
    }
  }
}
{% endfor %}