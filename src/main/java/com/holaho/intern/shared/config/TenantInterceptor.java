package com.holaho.intern.shared.config;

import com.holaho.intern.entity.Tenant;
import com.holaho.intern.shared.entity.BaseEntity;


import lombok.extern.slf4j.Slf4j;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hibernate {@link StatementInspector} that automatically appends a
 * {@code tenant_id} filter to SELECT queries for multi-tenant isolation.
 * <p>
 * <strong>Architecture & Security Design:</strong>
 * <ul>
 *   <li><strong>Transparent Interception:</strong> Existing Spring Data JPA repository queries
 *       automatically receive tenant filters without modifying custom finder methods.</li>
 *   <li><strong>Scope:</strong> Intercepts <code>SELECT</code> SQL statements produced by Hibernate ORM.</li>
 * </ul>
 * <p>
 * <strong>Critical Limitations & Guidelines (S-11):</strong>
 * <ul>
 *   <li><strong>Native Queries (INSERT/UPDATE/DELETE):</strong> This inspector only modifies <code>SELECT</code> queries.
 *       For <code>INSERT</code> or <code>UPDATE</code> statements executed via <code>@Modifying @Query(nativeQuery = true)</code>,
 *       developers <em>MUST</em> explicitly set <code>tenant_id = :tenantId</code> or rely on {@link BaseEntity} lifecycle hooks.</li>
 *   <li><strong>Cross-Tenant Admin Operations:</strong> System-level admin queries that intentionally bypass tenant isolation
 *       must clear the context using <code>TenantContext.clear()</code> before execution.</li>
 * </ul>
 */
@Slf4j
public class TenantInterceptor implements StatementInspector {

    private static final String[] TENANT_TABLES = {
            "users", "roles", "permissions", "departments", "mentors",
            "intern_profiles", "intern_documents", "applications",
            "application_reviews", "internship_contracts", "programs",
            "program_groups", "group_members", "tasks", "task_updates",
            "evaluations", "attendances", "support_tickets", "ticket_comments",
            "notifications", "weekly_reports", "refresh_tokens",
            "system_configs", "leave_requests", "allowances", "tenants"
    };

    @Override
    public String inspect(String sql) {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId <= 0) {
            return sql;
        }

        String lowerSql = sql.toLowerCase();

        // Only intercept SELECT statements
        if (!lowerSql.trim().startsWith("select")) {
            return sql;
        }

        // Check if the query involves any tenant-aware table (using whole-word matching)
        for (String table : TENANT_TABLES) {
            Pattern pattern = Pattern.compile("\\b" + table + "\\b");
            if (pattern.matcher(lowerSql).find()) {
                // Find the appropriate alias for the table
                String alias = findTableAlias(lowerSql, table);
                
                // Sanitize alias to ensure it only contains valid SQL identifier characters
                if (alias != null && !alias.matches("^[a-zA-Z0-9_]+$")) {
                    log.warn("Invalid table alias format detected: '{}'. Skipping tenant filter injection.", alias);
                    return sql;
                }

                // S-17 Safety: tenantId is a type-safe Long guaranteed > 0
                String tenantCondition = (alias != null)
                        ? alias + ".tenant_id=" + tenantId
                        : table + ".tenant_id=" + tenantId;

                int insertPos = findInsertPosition(lowerSql);
                boolean hasWhere = lowerSql.contains("where");

                if (hasWhere) {
                    // Insert " AND tenant_id=X" before tail keywords or at end
                    if (insertPos >= 0) {
                        sql = sql.substring(0, insertPos) + " AND " + tenantCondition + " " + sql.substring(insertPos);
                    } else {
                        sql = sql + " AND " + tenantCondition;
                    }
                } else {
                    // Insert " WHERE tenant_id=X" before tail keywords or at end
                    if (insertPos >= 0) {
                        sql = sql.substring(0, insertPos) + " WHERE " + tenantCondition + " " + sql.substring(insertPos);
                    } else {
                        sql = sql + " WHERE " + tenantCondition;
                    }
                }

                log.debug("Tenant-filtered SQL: {}", sql);
                break; // Only apply once per query
            }
        }

        return sql;
    }

    /**
     * Attempt to find the alias used for a table in the SQL statement.
     * Looks for patterns like "tablename alias" or "tablename as alias".
     */
    private String findTableAlias(String lowerSql, String table) {
        // Match table name as a whole word to avoid things like 'user_roles' matching 'roles'
        Pattern pattern = Pattern.compile("\\b" + table + "\\b");
        Matcher matcher = pattern.matcher(lowerSql);
        
        if (!matcher.find()) return null;
        int tableIdx = matcher.start();

        String afterTable = lowerSql.substring(tableIdx + table.length()).trim();

        // Pattern: "table alias" or "table as alias"
        if (afterTable.startsWith("as ")) {
            afterTable = afterTable.substring(3).trim();
        }

        // Extract the alias (next word before a space, comma, or join keyword)
        StringBuilder alias = new StringBuilder();
        for (char c : afterTable.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                alias.append(c);
            } else {
                break;
            }
        }

        String result = alias.toString();
        // Skip SQL keywords that are not aliases
        if (result.isEmpty() || isKeyword(result)) {
            return null;
        }
        return result;
    }

    private boolean isKeyword(String word) {
        return switch (word.toLowerCase()) {
            case "where", "on", "join", "inner", "left", "right", "cross",
                 "outer", "group", "order", "limit", "having", "set",
                 "values", "select", "from", "and", "or", "not", "in" -> true;
            default -> false;
        };
    }

    private int findInsertPosition(String lowerSql) {
        String[] keywords = {"order by", "group by", "having", "limit", "for update"};
        int earliest = -1;
        for (String kw : keywords) {
            int idx = lowerSql.indexOf(kw);
            if (idx >= 0 && (earliest < 0 || idx < earliest)) {
                earliest = idx;
            }
        }
        return earliest;
    }
}

