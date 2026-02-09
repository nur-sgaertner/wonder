package com.webobjects.eoaccess;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSSelector;

/**
 * Can be used to create a temporary {@link EOAttribute} for usage inside single
 * queries. It has a parent {@link EOEntity}, but won't call any mutation
 * methods on it.
 *
 * @author Copyright (c) 2026 NUREG. All rights reserved.
 */
public class ERXEOTempAttribute extends EOAttribute {

	public ERXEOTempAttribute(EOEntity entity, String definition) {
		super(entity, definition);
	}

	@Override
	protected void _parent_setIsEdited() {
		// don't touch the parent
	}

	@Override
	protected void _parent_removeAttribute(EOAttribute att) {
		// don't touch the parent
	}

	@Override
	protected void _setReadOnly(boolean yn) {
		// mostly copied from EOAttribute
		if (_flags_isReadOnly != yn) {
			if (!yn && isDerived() && !isFlattened()) {
				throw new IllegalArgumentException("Unable to remove read only on a derived not flattened attribute");
			}
			_flags_isReadOnly = yn;
			// don't touch the parent
			_flags_isNonUpdateableInitialized = false;
		}
	}

	@Override
	protected void _removeFromEntityArraySelector(NSArray oldArray, NSSelector sel) {
		// don't touch the parent
	}

}
