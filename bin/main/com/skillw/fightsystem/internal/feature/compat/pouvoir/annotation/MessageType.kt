package com.skillw.fightsystem.internal.feature.compat.pouvoir.annotation

import com.skillw.fightsystem.FightSystem
import com.skillw.fightsystem.api.fight.DamageType
import com.skillw.fightsystem.api.fight.FightData
import com.skillw.fightsystem.api.fight.message.Message
import com.skillw.fightsystem.api.fight.message.MessageBuilder
import com.skillw.fightsystem.internal.manager.FSConfig
import com.skillw.pouvoir.Pouvoir
import com.skillw.pouvoir.api.plugin.annotation.AutoRegister
import com.skillw.pouvoir.api.plugin.map.BaseMap
import com.skillw.pouvoir.api.script.annotation.ScriptAnnotation
import com.skillw.pouvoir.api.script.annotation.ScriptAnnotationData
import com.skillw.pouvoir.util.toArgs
import org.bukkit.entity.Player
import taboolib.common.platform.function.console
import taboolib.module.lang.sendLang
import javax.script.ScriptContext.ENGINE_SCOPE

/**
 * MessageType
 *
 * @constructor MessageType Key String
 */
@AutoRegister
internal object MessageType : ScriptAnnotation("MessageType", fileAnnotation = true) {
    private val messageCache = BaseMap<String, Message>()
    override fun handle(data: ScriptAnnotationData) {
        val script = data.script
        val args = data.args.toArgs()
        val function = data.function
        if (function != "null") return
        val vars = script.script.engine.getBindings(ENGINE_SCOPE)
        val key = vars["key"]?.toString() ?: error("MessageType key in ${script.key} is null")
        object : MessageBuilder {
            override val key: String = key

            override fun build(
                damageType: com.skillw.fightsystem.api.fight.DamageType,
                fightData: FightData,
                first: Boolean,
                type: Message.Type,
            ): Message {
                return messageCache.map.getOrPut(key) {
                    object : Message {
                        override val fightData: FightData = fightData

                        override fun sendTo(vararg players: Player) {
                            Pouvoir.scriptManager.invoke<Unit>(
                                script, "sendTo", parameters = arrayOf(players, fightData, first, type)
                            )
                        }

                        override fun plus(message: Message, type: Message.Type): Message {
                            return Pouvoir.scriptManager.invoke<Message>(
                                script, "plus", parameters = arrayOf(message, fightData, first, type)
                            ) ?: error("MessageType plus's returning value in ${script.key} is null")
                        }
                    }
                }
            }
        }.register()
        FSConfig.debug { console().sendLang("annotation-message-type-register", key) }
        script.onDeleted("MessageType-$key") {
            FSConfig.debug { console().sendLang("annotation-message-type-unregister", key) }
            com.skillw.fightsystem.FightSystem.messageBuilderManager.attack.remove(key)
            com.skillw.fightsystem.FightSystem.messageBuilderManager.defend.remove(key)
        }
    }
}