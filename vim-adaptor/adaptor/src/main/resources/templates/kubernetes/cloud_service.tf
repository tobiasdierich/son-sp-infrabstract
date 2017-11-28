{% for vdu in csd.getVirtualDeploymentUnits() %}
resource "kubernetes_replication_controller" "{{ vdu.getName() }}-{{ serviceId }}" {
  metadata {
    name = "{{ vdu.getName() }}-{{ serviceInstanceId }}"
    labels {
      service = "{{ serviceInstanceId }}"
      vdu = "{{ vdu.getId() }}"
    }
  }

  spec {
    selector {
      service = "{{ serviceInstanceId }}"
      vdu = "{{ vdu.getId() }}"
    }
    template {
      container {
        image = "{{ vdu.getServiceImage() }}"
        name  = "{{ vdu.getName() }}-{{ serviceInstanceId }}"

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

        {% for env in vdu.getEnvironmentVariables() %}
          env {
            name = "{{ env.getName() }}"
            value = "{{ env.getValue() }}"
          }
        {% endfor %}
      }
    }
  }
}

resource "kubernetes_service" "{{ vdu.getName() }}-{{ serviceId }}" {
  metadata {
    name = "{{ vdu.getName() }}-{{ serviceInstanceId }}"

    labels {
      service = "{{ serviceInstanceId }}"
      vdu = "{{ vdu.getId() }}"
    }
  }
  spec {
    selector {
      vdu = "${kubernetes_replication_controller.{{ vdu.getName() }}-{{ serviceId }}.metadata.0.labels.vdu}"
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

resource "kubernetes_horizontal_pod_autoscaler" "{{ vdu.getName() }}-{{ serviceId }}" {
  metadata {
    name = "{{ vdu.getName() }}-{{ serviceInstanceId }}"

    labels {
      service = "{{ serviceInstanceId }}"
      vdu = "{{ vdu.getId() }}"
    }
  }
  spec {
    max_replicas = {{ vdu.getScalingConfiguration().getMaximum() }}
    min_replicas = {{ vdu.getScalingConfiguration().getMinimum() }}
    scale_target_ref {
      kind = "ReplicationController"
      name = "${kubernetes_replication_controller.{{ vdu.getName() }}-{{ serviceId }}.metadata.0.name}"
    }
  }
}
{% endfor %}
