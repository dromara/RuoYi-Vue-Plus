import { DeleteOutlined<#if enableExport>, DownloadOutlined</#if>, EditOutlined, PlusOutlined, SortAscendingOutlined } from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
<#if needCheckbox>
  ProFormCheckbox,
</#if>
<#if needDateField>
  ProFormDateTimePicker,
</#if>
<#if needDigit>
  ProFormDigit,
</#if>
<#if needSelect>
  ProFormSelect,
</#if>
<#if needTextArea>
  ProFormTextArea,
</#if>
  ProFormText,
  ProFormTreeSelect,
  ProTable,
  type ActionType,
  type ProColumns
} from '@ant-design/pro-components';
import { useBoolean } from 'ahooks';
import { Button, Form, message<#if enableStatus || needSwitchField>, Switch</#if><#if enableSort>, InputNumber</#if> } from 'antd';
import { useMemo, useRef, useState } from 'react';
import type { ${BusinessName}Form, ${BusinessName}Query, ${BusinessName}VO } from '@/api/${moduleName}/${businessName}/types';
import {
  add${BusinessName},
<#if enableStatus>
  change${BusinessName}Status,
</#if>
  del${BusinessName},
  get${BusinessName},
  list${BusinessName},
<#if enableSort>
  update${BusinessName}Sort,
</#if>
  update${BusinessName}
} from '@/api/${moduleName}/${businessName}';
<#if needDict>
import DictTag from '@/components/common/DictTag';
</#if>
<#if needFileUpload>
import FileUpload from '@/components/common/FileUpload';
</#if>
<#if needImagePreview>
import ImagePreview from '@/components/common/ImagePreview';
</#if>
<#if needImageUpload>
import ImageUpload from '@/components/common/ImageUpload';
</#if>
<#if needEditor>
import RichTextEditor from '@/components/common/RichTextEditor';
</#if>
import RowActions from '@/components/common/RowActions';
<#if needDict>
import { useDict } from '@/hooks/useDict';
</#if>
<#if enableExport>
import { useTableExport } from '@/hooks/useTableExport';
</#if>
import { useTreeTableExpand } from '@/hooks/useTreeTableExpand';
import { useUserStore } from '@/stores/userStore';
<#if needDict>
import { dictOptions } from '@/utils/dict';
</#if>
<#if enableStatus>
import { confirmAction } from '@/utils/modal';
</#if>
import { hasPermi } from '@/utils/permission';
import { filterTree<#if needDateField>, formatDateTimeFields</#if>, handleTree<#if needDateField>, toDayjsFields</#if> } from '@/utils/ruoyi';

const default${BusinessName}Form: ${BusinessName}Form = {
  ${treeParentCode}: ${treeRootValueTsLiteral},
<#list columns as column>
<#if (column.insert || column.edit) && !column.pk && column.htmlType == "checkbox">
  ${column.javaField}: [],
</#if>
</#list>
};

interface ${BusinessName}SelectNode {
  title: string;
  value: string | number;
  children?: ${BusinessName}SelectNode[];
}

function toTreeSelectData(nodes: ${BusinessName}VO[]): ${BusinessName}SelectNode[] {
  return nodes.map(node => ({
    title: String(node.${treeName} || ''),
    value: node.${treeCode},
    children: node.children ? toTreeSelectData(node.children) : undefined
  }));
}

<#if enableStatus>
const ${statusField}ActiveValue = <#if statusColumn.javaType == "Boolean">true<#elseif statusColumn.javaType == "Integer" || statusColumn.javaType == "Long">0<#else>'0'</#if>;
const ${statusField}InactiveValue = <#if statusColumn.javaType == "Boolean">false<#elseif statusColumn.javaType == "Integer" || statusColumn.javaType == "Long">1<#else>'1'</#if>;

</#if>
export default function ${BusinessName}Page() {
  const actionRef = useRef<ActionType | undefined>(undefined);
  const [form] = Form.useForm<${BusinessName}Form>();
  const userInfo = useUserStore(state => state.userInfo);
<#if needDict>
  const dicts = useDict(${dicts});
</#if>
  const [treeOptions, setTreeOptions] = useState<${BusinessName}VO[]>([]);
  const [tableRows, setTableRows] = useState<${BusinessName}VO[]>([]);
  const { expandAll, expandedRowKeys, onExpandedRowsChange, syncExpandedRows, toggleExpandAll } =
    useTreeTableExpand<${BusinessName}VO>(row => row.${treeCode});
  const [modalOpen, { setTrue: openModal, setFalse: closeModal }] = useBoolean(false);
  const [modalTitle, setModalTitle] = useState('');
<#if enableExport>
  const { updateExportParams, exportFile } = useTableExport();
</#if>

  const canAdd = hasPermi(userInfo, ['${permissionPrefix}:add']);
  const canEdit = hasPermi(userInfo, ['${permissionPrefix}:edit']);
  const canRemove = hasPermi(userInfo, ['${permissionPrefix}:remove']);
<#if enableExport>
  const canExport = hasPermi(userInfo, ['${permissionPrefix}:export']);
</#if>
  const treeSelectData = useMemo(
    () => [{ title: '顶级节点', value: ${treeRootValueTsLiteral}, children: toTreeSelectData(treeOptions) }],
    [treeOptions]
  );

  const loadTreeOptions = async (excludeId?: string | number) => {
    const res = await list${BusinessName}();
    const rows = handleTree<${BusinessName}VO>(res.data || [], '${treeCode}', '${treeParentCode}');
    setTreeOptions(excludeId ? filterTree(rows, node => node.${treeCode} !== excludeId) : rows);
  };

  const openAdd = async (row?: ${BusinessName}VO) => {
    await loadTreeOptions();
    form.resetFields();
    form.setFieldsValue({ ...default${BusinessName}Form, ${treeParentCode}: row?.${treeCode} || treeRootValueTsLiteral });
    setModalTitle('添加${functionName}');
    openModal();
  };

  const openEdit = async (row: ${BusinessName}VO) => {
    await loadTreeOptions(row.${treeCode});
    const res = await get${BusinessName}(row.${pkColumn.javaField});
    const data = <#if needDateField>toDayjsFields({ ...res.data }, [
<#list columns as column>
<#if (column.insert || column.edit) && column.htmlType == "datetime">
      '${column.javaField}',
</#if>
</#list>
    ])<#else>{ ...res.data }</#if>;
<#list columns as column>
<#if (column.insert || column.edit) && column.htmlType == "checkbox">
    if (typeof data.${column.javaField} === 'string') {
      data.${column.javaField} = data.${column.javaField}.split(',');
    }
</#if>
</#list>
    form.resetFields();
    form.setFieldsValue(data);
    setModalTitle('修改${functionName}');
    openModal();
  };

  const submitForm = async (values: ${BusinessName}Form) => {
    const submitValues = <#if needDateField>formatDateTimeFields({ ...values }, [
<#list columns as column>
<#if (column.insert || column.edit) && column.htmlType == "datetime">
      '${column.javaField}',
</#if>
</#list>
    ])<#else>{ ...values }</#if>;
<#list columns as column>
<#if (column.insert || column.edit) && column.htmlType == "checkbox">
    if (Array.isArray(submitValues.${column.javaField})) {
      submitValues.${column.javaField} = submitValues.${column.javaField}.join(',');
    }
</#if>
</#list>
    submitValues.${pkColumn.javaField} ? await update${BusinessName}(submitValues) : await add${BusinessName}(submitValues);
    message.success('操作成功');
    form.resetFields();
    actionRef.current?.reload();
    return true;
  };

  const remove = async (row: ${BusinessName}VO) => {
    await del${BusinessName}(row.${pkColumn.javaField});
    message.success('删除成功');
    actionRef.current?.reload();
  };

<#if enableStatus>
  const handleStatusChange = async (row: ${BusinessName}VO, checked: boolean) => {
    const previousStatus = row.${statusField};
    const status = checked ? ${statusField}ActiveValue : ${statusField}InactiveValue;
    const text = checked ? '启用' : '停用';
    try {
      await confirmAction(`确认要"${r'${text}'}"吗?`);
      await change${BusinessName}Status(row.${pkColumn.javaField}, status);
      message.success(`${r'${text}'}成功`);
      actionRef.current?.reload();
    } catch {
      row.${statusField} = previousStatus;
      actionRef.current?.reload();
    }
  };

</#if>
<#if enableSort>
  const handleSortChange = async (row: ${BusinessName}VO, value?: number | null) => {
    await update${BusinessName}Sort(row.${pkColumn.javaField}, value || 0);
    message.success('排序更新成功');
    actionRef.current?.reload();
  };

</#if>
  const columns: ProColumns<${BusinessName}VO>[] = [
<#list columns as column>
<#if column.list && !column.pk>
<#if enableStatus && statusField == column.javaField>
    {
      title: '${column.columnLabel}',
      dataIndex: '${column.javaField}',
      valueType: 'select',
      width: 100,
<#if column.dictType?has_content>
      fieldProps: { options: dictOptions(dicts.${column.dictType}) },
</#if>
      render: (_, row) => (
        <Switch
          checked={row.${column.javaField} === ${statusField}ActiveValue}
          disabled={!canEdit}
          onChange={checked => handleStatusChange(row, checked)}
        />
      )
    },
<#elseif enableSort && sortField == column.javaField>
    {
      title: '${column.columnLabel}',
      dataIndex: '${column.javaField}',
      search: false,
      width: 130,
      render: (_, row) => <InputNumber min={0} value={row.${column.javaField} as number} onChange={value => handleSortChange(row, value)} />
    },
<#elseif column.htmlType == "datetime">
    { title: '${column.columnLabel}', dataIndex: '${column.javaField}', valueType: 'dateTime', search: <#if column.query>true<#else> false</#if>, width: 170 },
<#elseif column.htmlType == "imageUpload">
    {
      title: '${column.columnLabel}',
      dataIndex: '${column.javaField}Url',
      search: false,
      width: 100,
      render: (_, row) => <ImagePreview src={row.${column.javaField}Url || String(row.${column.javaField} || '')} width={50} height={50} />
    },
<#elseif column.dictColumn>
    {
      title: '${column.columnLabel}',
      dataIndex: '${column.javaField}',
      valueType: 'select',
      fieldProps: { options: dictOptions(dicts.${column.dictType}) },
      render: (_, row) => <DictTag options={dicts.${column.dictType}} value={row.${column.javaField}} />
    },
<#elseif column.htmlType == "switch">
    {
      title: '${column.columnLabel}',
      dataIndex: '${column.javaField}',
      valueType: 'select',
      width: 100,
      render: (_, row) => <Switch checked={row.${column.javaField} === ${column.switchActiveValue}} disabled />
    },
<#else>
    { title: '${column.columnLabel}', dataIndex: '${column.javaField}'<#if !column.query>, search: false</#if> },
</#if>
</#if>
</#list>
    {
      title: '操作',
      valueType: 'option',
      width: 130,
      fixed: 'right',
      render: (_, row) => (
        <RowActions
          actions={[
            canEdit && { key: 'edit', label: '修改', icon: <EditOutlined />, onClick: () => openEdit(row) },
            canAdd && { key: 'add', label: '新增', icon: <PlusOutlined />, onClick: () => openAdd(row) },
            canRemove && {
              key: 'delete',
              label: '删除',
              icon: <DeleteOutlined />,
              danger: true,
              confirm: `是否确认删除${functionName}编号为"${r'${row.'}${pkColumn.javaField}${r'}'}"的数据项？`,
              onClick: () => remove(row)
            }
          ]}
        />
      )
    }
  ];

  return (
    <PageContainer title="${functionName}">
      <ProTable<${BusinessName}VO, ${BusinessName}Query>
        actionRef={actionRef}
        rowKey="${treeCode}"
        columns={columns}
        scroll={{ x: 1000 }}
        pagination={false}
        search={{ labelWidth: 90 }}
        expandable={{
          expandedRowKeys,
          onExpandedRowsChange
        }}
        request={async params => {
          const res = await list${BusinessName}(params);
          const rows = handleTree<${BusinessName}VO>(res.data || [], '${treeCode}', '${treeParentCode}');
          setTableRows(rows);
          syncExpandedRows(rows, expandAll);
<#if enableExport>
          updateExportParams(params);
</#if>
          return { data: rows, total: rows.length, success: true };
        }}
        toolbar={{ title: '${functionName}列表' }}
        toolBarRender={() => [
          canAdd && (
            <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => openAdd()}>
              新增
            </Button>
          ),
          <Button
            key="expand"
            icon={<SortAscendingOutlined />}
            onClick={() => toggleExpandAll(tableRows)}
          >
            展开/折叠
          </Button><#if enableExport>,
          canExport && (
            <Button
              key="export"
              icon={<DownloadOutlined />}
              onClick={() => exportFile('/${moduleName}/${businessName}/export', () => `${businessName}_${r'${Date.now()}'}.xlsx`)}
            >
              导出
            </Button>
          )</#if>
        ]}
      />

      <ModalForm<${BusinessName}Form>
        title={modalTitle}
        open={modalOpen}
        width={560}
        form={form}
        layout="vertical"
        initialValues={default${BusinessName}Form}
        modalProps={{ destroyOnHidden: true, onCancel: closeModal }}
        onOpenChange={open => !open && closeModal()}
        onFinish={submitForm}
      >
        <ProFormText name="${pkColumn.javaField}" hidden />
<#list columns as column>
<#if (column.insert || column.edit) && !column.pk>
<#assign field = column.javaField>
<#if field == treeParentCode>
        <ProFormTreeSelect
          name="${column.javaField}"
          label="${column.columnLabel}"
          <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if>
          fieldProps={{
            allowClear: true,
            treeDefaultExpandAll: true,
            treeData: treeSelectData,
            placeholder: '请选择${column.columnLabel}'
          }}
        />
<#elseif column.htmlType == "input">
        <ProFormText name="${column.javaField}" label="${column.columnLabel}" <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if> />
<#elseif column.htmlType == "textarea">
        <ProFormTextArea name="${column.javaField}" label="${column.columnLabel}" <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if> />
<#elseif column.htmlType == "inputNumber">
        <ProFormDigit name="${column.javaField}" label="${column.columnLabel}" min={0} <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if> />
<#elseif (column.htmlType == "select" || column.htmlType == "radio" || column.htmlType == "switch") && column.dictType?has_content>
        <ProFormSelect name="${column.javaField}" label="${column.columnLabel}" options={dictOptions(dicts.${column.dictType})} <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if> />
<#elseif column.htmlType == "checkbox" && column.dictType?has_content>
        <ProFormCheckbox.Group name="${column.javaField}" label="${column.columnLabel}" options={dictOptions(dicts.${column.dictType})} <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if> />
<#elseif column.htmlType == "switch">
        <Form.Item
          name="${column.javaField}"
          label="${column.columnLabel}"
          valuePropName="checked"
          getValueProps={value => ({ checked: value === ${column.switchActiveValue} })}
          normalize={checked => (checked ? ${column.switchActiveValue} : ${column.switchInactiveValue})}
        >
          <Switch />
        </Form.Item>
<#elseif column.htmlType == "datetime">
        <ProFormDateTimePicker name="${column.javaField}" label="${column.columnLabel}" <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if> />
<#elseif column.htmlType == "imageUpload">
        <Form.Item name="${column.javaField}" label="${column.columnLabel}" <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if>>
          <ImageUpload />
        </Form.Item>
<#elseif column.htmlType == "fileUpload">
        <Form.Item name="${column.javaField}" label="${column.columnLabel}" <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if>>
          <FileUpload />
        </Form.Item>
<#elseif column.htmlType == "editor">
        <Form.Item name="${column.javaField}" label="${column.columnLabel}" <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if>>
          <RichTextEditor />
        </Form.Item>
<#else>
        <ProFormText name="${column.javaField}" label="${column.columnLabel}" <#if column.required>rules={[{ required: true, message: '${column.columnLabel}不能为空' }]}</#if> />
</#if>
</#if>
</#list>
      </ModalForm>
    </PageContainer>
  );
}




