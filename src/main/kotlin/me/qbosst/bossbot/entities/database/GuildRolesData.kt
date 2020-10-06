package me.qbosst.bossbot.entities.database

import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.tables.GuildRoleDataTable
import me.qbosst.bossbot.util.FixedCache
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class GuildRolesData private constructor(private val map: Map<GuildRoleDataTable.Type, Array<Long>>)
{
    fun getRoles(guild: Guild, type: GuildRoleDataTable.Type): List<Role>
    {
        return map[type]?.mapNotNull { guild.getRoleById(it) } ?: listOf()
    }

    companion object
    {
        private val cache = FixedCache<Long, GuildRolesData>(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())

        fun get(guild: Guild): GuildRolesData
        {
            return getCached(guild) ?: transaction { get(guild, this) }
        }

        fun getCached(guild: Guild): GuildRolesData?
        {
            return cache.get(guild.idLong)
        }

        fun get(guild: Guild, transaction: Transaction): GuildRolesData
        {
            return transaction()
            {
                val map = mutableMapOf<GuildRoleDataTable.Type, Array<Long>>()
                for(type in enumValues<GuildRoleDataTable.Type>())
                {
                    val list = mutableListOf<Long>()
                    val rs = GuildRoleDataTable.select { GuildRoleDataTable.guild_id.eq(guild.idLong) and GuildRoleDataTable.type.eq(type.ordinal) }
                            .execute(transaction)

                    while (rs?.next() == true)
                        list.add(rs.getLong(GuildRoleDataTable.role_id.name))

                    map[type] = list.toTypedArray()
                }

                val data = GuildRolesData(map)
                cache.put(guild.idLong, data)
                data
            }
        }

        /**
         *  Adds a list of roles to the database
         *
         *  @param roles The roles to add to the database
         *  @param type The purpose of these roles
         *  @return List of roles that have been added to the database. Roles that have already been added will be skipped out
         */
        fun add(roles: List<Role>, type: GuildRoleDataTable.Type): List<Role>
        {
            return transaction()
            {
                val added = mutableListOf<Role>()
                for(role in roles)
                {
                    val exists = GuildRoleDataTable
                            .select { GuildRoleDataTable.guild_id.eq(role.guild.idLong) and GuildRoleDataTable.role_id.eq(role.idLong) and GuildRoleDataTable.type.eq(type.ordinal) }
                            .fetchSize(1)
                            .map { true }
                            .singleOrNull() ?: false

                    if(!exists)
                    {
                        GuildRoleDataTable.insert()
                        {
                            it[GuildRoleDataTable.guild_id] = role.guild.idLong
                            it[GuildRoleDataTable.role_id] = role.idLong
                            it[GuildRoleDataTable.type] = type.ordinal
                        }
                        added.add(role)
                    }
                }
                added
            }
        }

        /**
         *  Removes a list of roles from the database
         *
         *  @param roles The roles to remove from the database
         *  @param type The purpose for these roles
         *  @return List of roles that have been removed from the database.
         */
        fun remove(roles: List<Role>, type: GuildRoleDataTable.Type): List<Role>
        {
            return transaction()
            {
                val removed = mutableListOf<Role>()
                for(role in roles)
                {
                    val exists = GuildRoleDataTable
                            .select { GuildRoleDataTable.guild_id.eq(role.guild.idLong) and GuildRoleDataTable.role_id.eq(role.idLong) and GuildRoleDataTable.type.eq(type.ordinal) }
                            .fetchSize(1)
                            .map { true }
                            .singleOrNull() ?: false

                    if(exists)
                    {
                        GuildRoleDataTable.deleteWhere { GuildRoleDataTable.guild_id.eq(role.guild.idLong) and GuildRoleDataTable.role_id.eq(role.idLong) and GuildRoleDataTable.type.eq(type.ordinal) }
                        removed.add(role)
                    }
                }
                removed
            }
        }
    }

}