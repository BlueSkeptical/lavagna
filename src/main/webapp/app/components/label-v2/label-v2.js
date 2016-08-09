(function () {

    'use strict';

    var components = angular.module('lavagna.components');

    components.component('lvgLabelV2', {
    	template: '<span>'
			+'<span ng-bind="::$ctrl.name"></span>'
			+'<span lvg-if-instant="::($ctrl.type !== \'NULL\')">: </span>'
			+'<lvg-label-val-v2 value-ref="$ctrl.value" project-metadata-ref="$ctrl.projectMetadata"></lvg-label-val-v2>'
			+'<span data-ng-transclude></span></span>',
    	bindings: {
    		valueRef: '&',
			projectMetadataRef:'&'
    	},
    	transclude: true,
    	controller: function() {
    		var ctrl = this;
    		ctrl.value = ctrl.valueRef();
    		ctrl.projectMetadata = ctrl.projectMetadataRef();
    		ctrl.type = ctrl.value.labelValueType || ctrl.value.type;
    		ctrl.name = (ctrl.projectMetadata && ctrl.projectMetadata.labels) ? ctrl.projectMetadata.labels[ctrl.value.labelId].name : ctrl.value.labelName;
    	}
    })
})();
