<template>
  <div style="background: white; border: 1px solid #e8e8e8;">
    <div class="card-header">
      Tests
    </div>
    <a-table
      :columns="columnsTests"
      :dataSource="testIncidents"
      :scroll="{ x: 0, y: 0 }"
      class="imis-table-no-pagination"
      rowKey="id"
    >
      <div slot="eventDate" slot-scope="eventDate">
        {{ getDate(eventDate) }}
      </div>

      <div slot="status" slot-scope="status">
        <a-icon
          :type="testResults.find((type) => type.id === status).icon"
          style="margin-right: 5px;"
        />
        {{ testResults.find((type) => type.id === status).label }}
      </div>

      <div slot="type" slot-scope="type">
        <a-icon
          :type="testTypes.find((testTypes) => testTypes.id === type).icon"
          style="margin-right: 5px;"
        />
        {{ testTypes.find((testTypes) => testTypes.id === type).label }}
      </div>
    </a-table>
  </div>
</template>

<script lang="ts">
import { Column } from 'ant-design-vue/types/table/column'
import { testResults } from '../../models/event-types'
import { testTypes } from '../../models/test-types'
import { getDate } from '../../util/helper-functions'
import { TestIncident } from '../../api/SwaggerApi'

const columnsTests: Partial<Column>[] = [
  {
    title: 'Test ID',
    dataIndex: 'testId',
    key: 'testId',
  },
  {
    title: 'Test Typ',
    dataIndex: 'type',
    key: 'type',
    scopedSlots: {
      customRender: 'type',
    },
  },
  {
    title: 'Test Status',
    dataIndex: 'status',
    key: 'status',
    scopedSlots: {
      customRender: 'status',
    },
  },
  {
    title: 'Aktualisiert',
    dataIndex: 'eventDate',
    key: 'eventDate',
    scopedSlots: {
      customRender: 'eventDate',
    },
  },
  {
    title: 'Kommentar',
    dataIndex: 'comment',
    key: 'comment',
  },
]

export default {
  name: 'TestIncidentsCard',
  props: { testIncidents: { type: Array as () => TestIncident[] } },
  data() {
    return {
      columnsTests,
      testResults,
      testTypes,
    }
  },
  methods: {
    getDate,
  },
}
</script>

<style scoped>
.card-header {
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
  font-size: 16px;
  border-bottom: 1px solid #e8e8e8;
}
</style>
