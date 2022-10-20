// Ready translated locale messages
const translationStrings = {
  en_US: {
    Edit: 'Edit',
    Save: 'Save',
    Delete: 'Delete',
    Cancel: 'Cancel',
    Revert: 'Revert',
    CreateAcl: 'Create ACL',
    CreateAclName: 'ACL Description',
    CreateAclTitle: 'Create Key Storage ACL for the project',
    'Edit Nodes': 'Edit Nodes',
    'Modify': 'Modify',
    'Edit Node Sources': 'Edit Node Sources',
    'The Node Source had an error': 'The Node Source had an error',
    'Validation errors': 'Validation errors',
    'empty.message.default': 'None configured. Click {0} to add a new plugin.',

    'unauthorized.status.help.1': 'Some Node Source returned an "Unauthorized" message.',
    'unauthorized.status.help.2': 'The Node Source plugin might need access to the Key Storage Resource. it could be enabled by Access Control Policy entries.',
    'unauthorized.status.help.3': 'Please be sure that the ACL policies enable "read" access to the Key Storage in this project for the project URN path (urn:project:name). ',
    'unauthorized.status.help.4': 'Go to {0} to create a Project ACL ',
    'unauthorized.status.help.5': 'Go to {0} to create a System ACL ',

    'acl.config.link.title': 'Project Settings > Access Control',
    'acl.config.system.link.title': 'System Settings > Access Control',
    'acl.example.summary': 'Example ACL Policy',

    uiv: {
      modal: {
        cancel: "Cancel",
        ok: "OK"
      }
    }
  },
  es: {
    Edit: 'Editar',
    Save: 'Guardar',
    Delete: 'Borrar',
    Cancel: 'Cancelar',
    Revert: 'Revertir',
    CreateAcl: 'Crear ACL',
    CreateAclName: 'Descripción de la ACL',
    CreateAclTitle: 'Crear Key Storage ACL para el proyecto',
    'Edit Nodes': 'Editar Nodos',
    'Modify': 'Modificar',
    'Edit Node Sources': 'Editar Configuracion de Nodes',
    'The Node Source had an error': 'La configuracion tiene errores',
    'Validation errors': 'Validación de errores',
    'empty.message.default': 'Sin configuracion. Click  en {0} para agregar un nuevo plugin.',

    'unauthorized.status.help.1': 'Un plugin Node Source retornó un "Unauthorized" mensaje .',
    'unauthorized.status.help.2': 'El plugin Node Source podria necesitar acceso al Key Storage. Se podria habilitar creando una nueva ACL Policy, or alguna ACL existente necesita ser modificada.',
    'unauthorized.status.help.3': 'Por favor asegurate de que la ACL Policy tiene access de lectura ("read") para el Key Storage en este proyecto ( urn:project:name). ',
    'unauthorized.status.help.4': 'Ir a {0} para crear una ACL Policy al nivel de proyecto. ',
    'unauthorized.status.help.5': 'Ir a {0} para crear una ACL Policy a nivel del sistema. ',

    'acl.config.link.title': 'Project Settings > Access Control',
    'acl.config.system.link.title': 'System Settings > Access Control',

    'acl.example.summary': 'Ejemplo de ACL Policy',
    uiv: {
      modal: {
        cancel: "Cancelar",
        ok: "OK"
      }
    }
  },
}

export default {
  messages: translationStrings
}
