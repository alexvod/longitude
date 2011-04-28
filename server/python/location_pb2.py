# Generated by the protocol buffer compiler.  DO NOT EDIT!

from google.protobuf import descriptor
from google.protobuf import message
from google.protobuf import reflection
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)



DESCRIPTOR = descriptor.FileDescriptor(
  name='location.proto',
  package='longitude',
  serialized_pb='\n\x0elocation.proto\x12\tlongitude\"D\n\x08Location\x12\x0c\n\x04name\x18\x01 \x01(\t\x12\x0b\n\x03lat\x18\x02 \x02(\x01\x12\x0b\n\x03lng\x18\x03 \x02(\x01\x12\x10\n\x08\x61ltitude\x18\x04 \x01(\x01\"5\n\x0cLocationInfo\x12%\n\x08location\x18\x01 \x03(\x0b\x32\x13.longitude.LocationB \n\x15org.alexvod.longitudeB\x05ProtoH\x03')




_LOCATION = descriptor.Descriptor(
  name='Location',
  full_name='longitude.Location',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='name', full_name='longitude.Location.name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=unicode("", "utf-8"),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='lat', full_name='longitude.Location.lat', index=1,
      number=2, type=1, cpp_type=5, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='lng', full_name='longitude.Location.lng', index=2,
      number=3, type=1, cpp_type=5, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='altitude', full_name='longitude.Location.altitude', index=3,
      number=4, type=1, cpp_type=5, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=29,
  serialized_end=97,
)


_LOCATIONINFO = descriptor.Descriptor(
  name='LocationInfo',
  full_name='longitude.LocationInfo',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='location', full_name='longitude.LocationInfo.location', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=99,
  serialized_end=152,
)

_LOCATIONINFO.fields_by_name['location'].message_type = _LOCATION
DESCRIPTOR.message_types_by_name['Location'] = _LOCATION
DESCRIPTOR.message_types_by_name['LocationInfo'] = _LOCATIONINFO

class Location(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _LOCATION
  
  # @@protoc_insertion_point(class_scope:longitude.Location)

class LocationInfo(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _LOCATIONINFO
  
  # @@protoc_insertion_point(class_scope:longitude.LocationInfo)

# @@protoc_insertion_point(module_scope)
