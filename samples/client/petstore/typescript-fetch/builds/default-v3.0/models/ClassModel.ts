/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI Petstore
 * This spec is mainly for testing Petstore server and contains fake endpoints, models. Please do not use this for any other purpose. Special characters: \" \\
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { mapValues } from '../runtime';
/**
 * Model for testing model with "_class" property
 * @export
 * @interface ClassModel
 */
export interface ClassModel {
    /**
     * 
     * @type {string}
     * @memberof ClassModel
     */
    _class?: string;
}

/**
 * Check if a given object implements the ClassModel interface.
 */
export function instanceOfClassModel(value: object): value is ClassModel {
    return true;
}

export function ClassModelFromJSON(json: any): ClassModel {
    return ClassModelFromJSONTyped(json, false);
}

export function ClassModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ClassModel {
    if (json == null) {
        return json;
    }
    return {
        
        '_class': json['_class'] == null ? undefined : json['_class'],
    };
}

export function ClassModelToJSON(json: any): ClassModel {
    return ClassModelToJSONTyped(json, false);
}

export function ClassModelToJSONTyped(value?: ClassModel | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        '_class': value['_class'],
    };
}

