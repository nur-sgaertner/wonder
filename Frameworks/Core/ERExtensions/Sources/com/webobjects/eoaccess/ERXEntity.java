/**
 * 
 */
package com.webobjects.eoaccess;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.webobjects.eocontrol.EOClassDescription;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation._NSArrayUtilities;

import er.extensions.eof.ERXEOAccessUtilities;

/**
 * ERXEntity provides a basic subclass of EOEntity providing
 * a simple fix for vertical inheritance.
 * <p>
 * <b>Note:</b> If you plan on subclassing EOEntity or ERXEntity you
 * MUST put your subclass in the same package if you want it
 * to work. There are numerous default and protected instance
 * methods within EOEntity itself that will fail to resolve at
 * runtime if your subclass is in another package!
 * 
 * @see EOEntity
 * @author ldeck
 */
public class ERXEntity extends EOEntity {

	private static final Pattern NeededByEOFPattern = Pattern.compile( "\\QNeededByEOF\\E(\\d+)" );
	
	/**
	 * Creates and returns a new ERXEntity.
	 */
	public ERXEntity() {
		super();
	}

	/**
	 * Creates and returns a new EOEntity initialized from the
	 * property list plist belonging to the EOModel owner.
	 * plist is dictionary containing only property list data
	 * types (that is, NSDictionary, NSArray, NSData, and String).
	 * This constructor is used by EOModeler when it reads in an
	 * EOModel from a file.
	 * 
	 * @param plist - A dictionary of property list values from which to initialize the new EOEntity object.
	 * @param owner - The EOModel to which the newly created entity belongs.
	 * 
	 * @see EOPropertyListEncoding#encodeIntoPropertyList(NSMutableDictionary propertyList)
	 * @see EOPropertyListEncoding#awakeWithPropertyList(NSDictionary propertyList)
	 */
	public ERXEntity(NSDictionary plist, Object owner) {
		super(plist, owner);
	}
	
	/**
	 * ldeck radar bug#6302622.
	 * <p>
	 * Relating two sub-entities in vertical inheritance can fail to resolve
	 *  the foreign key for inserts. i.e., NeededByEOF&lt;index&gt; was not dealt with.
	 *  The simple fix is to return the primary key attribute at the given index.
	 * 
	 * @see com.webobjects.eoaccess.EOEntity#anyAttributeNamed(java.lang.String)
	 */
	@Override
	public EOAttribute anyAttributeNamed(String name) {
		Matcher matcher = null;
		EOAttribute result = super.anyAttributeNamed(name);
		if (result == null && name != null) {
			if ((matcher = NeededByEOFPattern.matcher(name)).matches()) {
				int neededIndex = Integer.valueOf(matcher.group(1));
				if (neededIndex >= primaryKeyAttributeNames().count()) {
					throw new IllegalStateException("No matching primary key found for entity'" + name() + "' with attribute'" + name + "'");
				}
				result = primaryKeyAttributes().objectAtIndex(neededIndex);
			} else {
				for (EOEntity subEntity : subEntities()) {
					result = subEntity.anyAttributeNamed(name);
					if (result != null) {
						break;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * @see com.webobjects.eoaccess.EOEntity#hasExternalName()
	 * @since 5.4.1
	 */
	@Override
	public boolean hasExternalName() {
		// (ldeck) radar://6592526 fix for 5.4.3 regression which assumed that any parent entity that is abstract has no external name!
		return externalName() != null && externalName().trim().length() > 0;
	}
	
	/**
	 * Sets the class description for the instance.
	 * 
	 * @param classDescription - the EOClassDescription to associate with the receiver.
	 */
	public void setClassDescription(EOClassDescription classDescription) {
		_classDescription = classDescription;
	}

	/**
	 * Overridden through our bottleneck.
	 */
	@Override
	protected EOKeyGlobalID _globalIDWithoutTypeCoercion(Object[] values) {
		return ERXSingleValueID.globalIDWithEntityName(name(), values);
	}

	public NSArray<EOAttribute> classAttributes() {
		NSMutableArray<EOAttribute> found = new NSMutableArray<>();
		for (String name : (NSArray<String>)this.classPropertyNames()) {
			if (this.attributeNamed(name) != null)
				found.add(this.attributeNamed(name));
		}
		return found.immutableClone();
	}

	public NSArray<EORelationship> classRelationships() {
                NSMutableArray<EORelationship> found = new NSMutableArray<>();
		for (String name : (NSArray<String>)this.classPropertyNames()) {
			if (this.relationshipNamed(name) != null)
				found.add(this.relationshipNamed(name));
		}
		return found.immutableClone();
	}

	/**
	 * Registers all relationships of subentities as hidden relationships. This enables you
	 * to create qualifiers on a parent entity that includes relationships that are only
	 * defined in a subentity.
	 * 
	 * @see #_hiddenRelationships()
	 */
	public void updateHiddenRelationshipsForInheritance() {
		if (!subEntities().isEmpty()) {
			NSMutableArray<EORelationship> hiddenRelationships = _hiddenRelationships();
			synchronized (hiddenRelationships) {
				NSArray<EOEntity> subEntities = ERXEOAccessUtilities.allSubEntitiesForEntity(this, false);
				for (EOEntity subEntity : subEntities) {
					for (EORelationship relationship : subEntity.relationships()) {
						if (!hiddenRelationships.contains(relationship)) {
							hiddenRelationships.add(relationship);
						}
					}
				}
			}
		}
	}

	/**
	 * Overridden to make this more thread-safe. Previously we sometimes get an
	 * NPE at EOEntity._globalIDForRowIsFinal(EOEntity.java:2868).
	 */
	@Override
	public NSArray<String> primaryKeyAttributeNames() {
		synchronized (EOModel._EOGlobalModelLock) {
			NSArray<String> result = this._primaryKeyAttributeNames;
			if (result == null) {
				this._primaryKeyAttributeNames = result = _NSArrayUtilities.resultsOfPerformingSelector(this.primaryKeyAttributes(), _NSArrayUtilities._nameSelector);
			}
			return result;
		}
	}

	/**
	 * Overridden to make this more thread-safe. Previously we sometimes get an
	 * NPE at EOCustomObject.snapshot(EOCustomObject.java:509).
	 */
	@Override
	NSArray<String> classPropertyToOneRelationshipNames() {
		synchronized (EOModel._EOGlobalModelLock) {
			NSArray<String> result = this._classPropertyToOneRelationshipNames;
			if (result == null) {
				NSMutableArray<String> list = new NSMutableArray();
				Iterator i$ = this.classProperties().iterator();
				while (i$.hasNext()) {
					EOProperty property = (EOProperty) i$.next();
					if (property instanceof EORelationship && !((EORelationship) property).isToMany()) {
						list.add(property.name());
					}
				}

				this._classPropertyToOneRelationshipNames = result = list;
			}
			return result;
		}
	}

	/**
	 * Overridden to make this more thread-safe. See
	 * classPropertyToOneRelationshipNames.
	 */
	@Override
	NSArray<String> classPropertyToManyRelationshipNames() {
		synchronized (EOModel._EOGlobalModelLock) {
			NSMutableArray<String> result = this._classPropertyToManyRelationshipNames;
			if (result == null) {
				NSMutableArray<String> list = new NSMutableArray();
				Iterator i$ = this.classProperties().iterator();
				while (i$.hasNext()) {
					EOProperty property = (EOProperty) i$.next();
					if (property instanceof EORelationship && ((EORelationship) property).isToMany()) {
						list.add(property.name());
					}
				}

				this._classPropertyToManyRelationshipNames = result = list;
			}
			return result;
		}
	}
}
