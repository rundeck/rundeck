const falsy = /^(?:f(?:alse)?|no?|0+)$/i;
export function ParseBool(val) { 
    return !falsy.test(val) && !!val;
};