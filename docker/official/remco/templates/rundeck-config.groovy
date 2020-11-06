{%- if exists("/rundeck/mail/props") %}
grails {
  mail {
    props = {{ getv("/rundeck/mail/props") }}
  }
}
{% endif %}
