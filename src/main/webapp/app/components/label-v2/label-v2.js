(function () {

    'use strict';

    var components = angular.module('lavagna.components');

    components.component('lvgLabelV2', {
    	template: '<lvg-label-val-v2 value-ref="$ctrl.value" project-metadata-ref="$ctrl.projectMetadata"></lvg-label-val-v2><span data-ng-transclude></span>',
    	bindings: {
    		valueRef: '&',
			projectMetadataRef:'&'
    	},
    	transclude: true,
    	controller: ['$window', '$element', lvgLabelV2Ctrl]
    })
    
    function lvgLabelV2Ctrl($window, $element) {
    	const ctrl = this;
    	const domElem = $element[0];
    	ctrl.value = ctrl.valueRef();
    	ctrl.projectMetadata = ctrl.projectMetadataRef();
    	
    	
    	
    	ctrl.$postLink = function lvgLabelV2PostLink() {
    		const addSeparator = (ctrl.value.labelValueType || ctrl.value.type) !== 'NULL';
        	const name = (ctrl.projectMetadata && ctrl.projectMetadata.labels) ? ctrl.projectMetadata.labels[ctrl.value.labelId].name : ctrl.value.labelName;
    		const nameAndSeparator = $window.document.createTextNode(name + (addSeparator ? ': ' : '' ));
    		domElem.insertBefore(nameAndSeparator, domElem.firstChild);
    	}    	
    }
    
})();
