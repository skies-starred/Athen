package foo.starred.athen.config.dsl.impl.builders.group

import foo.starred.athen.config.data.impl.ConfigGroupElementData
import foo.starred.athen.config.dsl.base.ConfigScope
import foo.starred.athen.config.dsl.base.ElementBuilder
import foo.starred.athen.config.dsl.impl.builders.config.ConfigMainBuilder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ConfigGroupBuilder(
    override val builder: ConfigMainBuilder,
    private val name: String,
    private val collapsed: Boolean = true,
    parent0: String? = null
) : ElementBuilder<ConfigGroupBuilder>(parent0), ConfigScope, ReadOnlyProperty<Any?, ConfigGroupBuilder> {
    override val parent: String
        get() = key

    lateinit var key: String
        private set

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, ConfigGroupBuilder> {
        key = "${builder.key}.${property.name}"
        builder.feature.option(ConfigGroupElementData(name, key, collapsed, super<ElementBuilder>.parent, description))

        return this
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): ConfigGroupBuilder {
        return this
    }
}
